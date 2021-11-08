package org.commcare.formplayer.application;

import io.sentry.Sentry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.util.InvalidCaseGraphException;
import org.commcare.formplayer.annotations.ConfigureStorageFromSession;
import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.api.json.JsonActionUtils;
import org.commcare.formplayer.api.process.FormRecordProcessorHelper;
import org.commcare.formplayer.api.util.ApiConstants;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.OpenRosaResponse;
import org.commcare.formplayer.beans.SubmitRequestBean;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.ErrorBean;
import org.commcare.formplayer.engine.FormplayerTransactionParserFactory;
import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.json.JSONObject;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.commcare.formplayer.objects.SerializableFormSession.SubmitStatus.PROCESSED_STACK;
import static org.commcare.formplayer.objects.SerializableFormSession.SubmitStatus.PROCESSED_XML;

/**
 * Controller class (API endpoint) containing all form entry logic. This includes
 * opening a new form, question answering, and form submission.
 */
@RestController
@EnableAutoConfiguration
public class FormSubmissionController extends AbstractBaseController {

    @Autowired
    private SubmitService submitService;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    @Autowired
    private RedisTemplate redisVolatilityDict;

    @Autowired
    private FormplayerDatadog datadog;

    @Resource(name = "redisVolatilityDict")
    private ValueOperations<String, FormVolatilityRecord> volatilityCache;

    private final Log log = LogFactory.getLog(FormSubmissionController.class);

    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean,
                                         @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken, HttpServletRequest request) throws Exception {
        FormSubmissionContext context = getFormProcessingContext(request, submitRequestBean);

        ProcessingStep.StepFactory stepFactory = new ProcessingStep.StepFactory(context, formSessionService);
        Stream<ProcessingStep> processingSteps = Stream.of(
                stepFactory.makeStep("validateAnswers", this::validateAnswers),
                stepFactory.makeStep("processFormXml", this::processFormXml, PROCESSED_XML),
                stepFactory.makeStep("updateVolatility", this::updateVolatility),
                stepFactory.makeStep("performSync", this::performSync),
                stepFactory.makeStep("doEndOfFormNav", this::doEndOfFormNav, PROCESSED_STACK)
        );

        // execute steps one at a time, only proceeding to the next step if the previous step was successful
        Optional<SubmitResponseBean> error = processingSteps
                .map((step) -> executeStep(request, step))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        if (error.isPresent()) {
            return error.get();
        }

        // Only delete session immediately after successful submit
        deleteSession(submitRequestBean.getSessionId());

        return context.getResponse();
    }

    public FormSubmissionContext getFormProcessingContext(HttpServletRequest request, SubmitRequestBean submitRequestBean) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(submitRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession, restoreFactory, formSendCalloutHandler, storageFactory);

        // add tags for future datadog/sentry requests
        datadog.addRequestScopedTag(Constants.FORM_NAME_TAG, serializableFormSession.getTitle());
        Sentry.setTag(Constants.FORM_NAME_TAG, serializableFormSession.getTitle());

        // package additional args to pass to category timing helper
        Map<String, String> extras = new HashMap();
        extras.put(Constants.DOMAIN_TAG, submitRequestBean.getDomain());

        return new FormSubmissionContext(request, submitRequestBean, formEntrySession, extras);
    }

    /**
     * Execute a step in the process and return an error response to halt processing or
     * and empty response to continue.
     *
     * @param request The HTTP Request object
     * @param step A supplier object that performs one unit of form processing and returns
     *             a SubmitResponseBean.
     * @return Empty Optional if the processing should continue otherwise an Optional containing the
     *          error response.
     */
    private Optional<SubmitResponseBean> executeStep(HttpServletRequest request, ProcessingStep step) {
        SubmitResponseBean response = null;
        try {
            response = step.execute();
        } catch (Exception e) {
            response = getErrorResponse(
                    request, Constants.ANSWER_RESPONSE_STATUS_NEGATIVE,
                    e.getMessage(), e);
        }
        if (response.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE)) {
            return Optional.empty();  // continue processing
        }
        log.debug(String.format("Aborting execution of processing steps after error in step: %s", step));
        return Optional.of(response);
    }

    private SubmitResponseBean validateAnswers(FormSubmissionContext context) {
        categoryTimingHelper.timed(
                Constants.TimingCategories.VALIDATE_SUBMISSION,
                () -> validateSubmitAnswers(context),
                context.getMetricsTags()
        );
        if (!context.getResponse().getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE)
                || !context.getRequest().isPrevalidated()) {
            return context.error(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
        }
        return context.success();
    }

    private SubmitResponseBean getErrorResponse(
            HttpServletRequest request,
            String status,
            String message,
            Throwable exception) {
        SubmitResponseBean responseBean = new SubmitResponseBean(status);
        NotificationMessage notification = new NotificationMessage(
                message,
                true,
                NotificationMessage.Tag.submit);
        responseBean.setNotification(notification);
        logNotification(notification, request);
        log.error(message, exception);
        return responseBean;
    }

    private SubmitResponseBean processFormXml(FormSubmissionContext context) {
        try {
            restoreFactory.setAutoCommit(false);
            processXmlInner(context);

            String response = submitService.submitForm(
                    context.getFormEntrySession().getInstanceXml(),
                    context.getFormEntrySession().getPostUrl()
            );
            parseSubmitResponseMessage(response, context.getResponse());

            restoreFactory.commit();
        } catch (InvalidCaseGraphException e) {
            return getErrorResponse(
                    context.getHttpRequest(), Constants.SUBMIT_RESPONSE_CASE_CYCLE_ERROR,
                    "Form submission failed due to a cyclic case relationship. " +
                            "Please contact the support desk to help resolve this issue.", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            return context.error(Constants.SUBMIT_RESPONSE_TOO_MANY_REQUESTS);
        } catch (HttpClientErrorException e) {
            return getErrorResponse(
                    context.getHttpRequest(), "error",
                    String.format("Form submission failed with error response: %s, %s, %s",
                            e.getMessage(), e.getResponseBodyAsString(), e.getResponseHeaders()),
                    e);
        } catch (Exception e) {
            return getErrorResponse(
                    context.getHttpRequest(), "error",
                    e.getMessage(), e);
        } finally {
            // If autoCommit hasn't been reset to `true` by the commit() call then an error occurred
            if (!restoreFactory.getAutoCommit()) {
                // rollback sets autoCommit back to `true`
                restoreFactory.rollback();
            }
        }
        return context.success();
    }

    private void processXmlInner(FormSubmissionContext context) throws Exception {
        FormplayerTransactionParserFactory factory = new FormplayerTransactionParserFactory(
                restoreFactory.getSqlSandbox(),
                storageFactory.getPropertyManager().isBulkPerformanceEnabled()
        );
        FormRecordProcessorHelper.processXML(factory, context.getFormEntrySession().submitGetXml());
        categoryTimingHelper.timed(
            Constants.TimingCategories.PURGE_CASES,
            () -> {
                if (factory.wereCaseIndexesDisrupted() && storageFactory.getPropertyManager().isAutoPurgeEnabled()) {
                    FormRecordProcessorHelper.purgeCases(factory.getSqlSandbox());
                }
            },
            context.getMetricsTags()
        );
    }

    private SubmitResponseBean doEndOfFormNav(FormSubmissionContext context) {
        FormSession formEntrySession = context.getFormEntrySession();
        Object nextScreen = categoryTimingHelper.timed(
            Constants.TimingCategories.END_OF_FORM_NAV,
            () -> {
                if (formEntrySession.getMenuSessionId() != null &&
                        !("").equals(formEntrySession.getMenuSessionId().trim())) {
                    return doEndOfFormNav(menuSessionService.getSessionById(formEntrySession.getMenuSessionId()));
                }
                return null;
            },
            context.getMetricsTags()
        );
        context.getResponse().setNextScreen(nextScreen);
        return context.success();
    }

    private SubmitResponseBean performSync(FormSubmissionContext context) {
        boolean suppressAutosync = context.getFormEntrySession().getSuppressAutosync();

        if (storageFactory.getPropertyManager().isSyncAfterFormEnabled() && !suppressAutosync) {
            //If configured to do so, do a sync with server now to ensure dats is up to date.
            //Need to do before end of form nav triggers, since the new data might change the
            //validity of the form

            boolean skipFixtures = storageFactory.getPropertyManager().skipFixturesAfterSubmit();
            try {
                restoreFactory.performTimedSync(true, skipFixtures, false);
            } catch (Exception e) {
                return getErrorResponse(
                        context.getHttpRequest(), Constants.ANSWER_RESPONSE_STATUS_NEGATIVE,
                        e.getMessage(), e);
            }
        }
        return context.success();
    }

    private SubmitResponseBean updateVolatility(FormSubmissionContext context) {
        FormVolatilityRecord volatilityRecord = context.getFormEntrySession().getSessionVolatilityRecord();
        if (volatilityCache != null && volatilityRecord != null) {
            FormVolatilityRecord existingRecord = volatilityCache.get(volatilityRecord.getKey());
            if (existingRecord != null && existingRecord.matchesUser(context.getFormEntrySession())) {
                volatilityRecord = existingRecord;
            }
            volatilityRecord.updateFormSubmitted(context.getFormEntrySession());
            volatilityRecord.write(volatilityCache);
        }
        return context.success();
    }

    private void parseSubmitResponseMessage(String responseBody, SubmitResponseBean submitResponseBean) {
        if (responseBody != null) {
            try {
                Serializer serializer = new Persister();
                OpenRosaResponse openRosaResponse = serializer.read(OpenRosaResponse.class, responseBody);
                if (openRosaResponse != null && openRosaResponse.getMessage() != null) {
                    submitResponseBean.setSubmitResponseMessage(openRosaResponse.getMessage());
                }
            } catch (Exception e) {
                log.error("Exception parsing submission response body", e);
            }
        }
    }

    protected void deleteSession(String id) {
        formSessionService.deleteSessionById(id);
    }

    private Object doEndOfFormNav(SerializableMenuSession serializedSession) throws Exception {
        log.info("End of form navigation with serialized menu session: " + serializedSession);
        MenuSession menuSession = menuSessionFactory.buildSession(serializedSession);
        return runnerService.resolveFormGetNext(menuSession);
    }

    /**
     * Iterate over all answers and attempt to save them to check for validity.
     * Submit the complete XML instance to HQ if valid.
     */
    private void validateSubmitAnswers(FormSubmissionContext context) {
        HashMap<String, ErrorBean> errors = FormController.validateAnswers(
                context.getFormEntrySession().getFormEntryController(),
                context.getFormEntrySession().getFormEntryModel(),
                context.getRequest().getAnswers(),
                context.getFormEntrySession().getSkipValidation());
        if (errors.size() > 0) {
            context.error(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE, errors);
        }
    }
}
