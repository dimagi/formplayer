package org.commcare.formplayer.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.sentry.Sentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.util.InvalidCaseGraphException;
import org.commcare.formplayer.utils.CheckedSupplier;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.json.JSONObject;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.commcare.formplayer.annotations.ConfigureStorageFromSession;
import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.api.json.JsonActionUtils;
import org.commcare.formplayer.api.process.FormRecordProcessorHelper;
import org.commcare.formplayer.api.util.ApiConstants;
import org.commcare.formplayer.beans.AnswerQuestionRequestBean;
import org.commcare.formplayer.beans.ChangeLocaleRequestBean;
import org.commcare.formplayer.beans.FormEntryNavigationResponseBean;
import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.GetInstanceResponseBean;
import org.commcare.formplayer.beans.InstanceXmlBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.NewSessionRequestBean;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.OpenRosaResponse;
import org.commcare.formplayer.beans.RepeatRequestBean;
import org.commcare.formplayer.beans.SessionRequestBean;
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
import org.springframework.web.client.HttpClientErrorException;

/**
 * Controller class (API endpoint) containing all form entry logic. This includes
 * opening a new form, question answering, and form submission.
 */
@RestController
@EnableAutoConfiguration
public class FormController extends AbstractBaseController {

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

    @Value("${commcarehq.host}")
    private String host;

    private final Log log = LogFactory.getLog(FormController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @RequestMapping(value = Constants.URL_NEW_SESSION, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    public NewFormResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean,
                                           @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        String postUrl = host + newSessionBean.getPostUrl();
        return newFormResponseFactory.getResponse(newSessionBean, postUrl);
    }

    @RequestMapping(value = Constants.URL_CHANGE_LANGUAGE, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryResponseBean changeLocale(@RequestBody ChangeLocaleRequestBean changeLocaleBean,
                                              @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(changeLocaleBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession, restoreFactory, formSendCalloutHandler, storageFactory);
        formEntrySession.changeLocale(changeLocaleBean.getLocale());
        FormEntryResponseBean responseBean = formEntrySession.getCurrentJSON();
        updateSession(formEntrySession);
        responseBean.setTitle(serializableFormSession.getTitle());
        return responseBean;
    }

    @RequestMapping(value = Constants.URL_ANSWER_QUESTION, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryResponseBean answerQuestion(@RequestBody AnswerQuestionRequestBean answerQuestionBean,
                                                @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {

        SerializableFormSession serializableFormSession = categoryTimingHelper.timed(
                Constants.TimingCategories.GET_SESSION,
                () -> formSessionService.getSessionById(answerQuestionBean.getSessionId())
        );

        // add tags for future datadog/sentry requests
        datadog.addRequestScopedTag(Constants.FORM_NAME_TAG, serializableFormSession.getTitle());
        Sentry.setTag(Constants.FORM_NAME_TAG, serializableFormSession.getTitle());

        FormSession formEntrySession = categoryTimingHelper.timed(
                Constants.TimingCategories.INITIALIZE_SESSION,
                () -> new FormSession(serializableFormSession, restoreFactory, formSendCalloutHandler, storageFactory)
        );

        FormEntryResponseBean responseBean = categoryTimingHelper.timed(
                Constants.TimingCategories.PROCESS_ANSWER,
                () -> formEntrySession.answerQuestionToJSON(
                        answerQuestionBean.getAnswer(), answerQuestionBean.getFormIndex()
                )
        );

        categoryTimingHelper.timed(
                Constants.TimingCategories.VALIDATE_ANSWERS,
                () -> {
                    HashMap<String, ErrorBean> errors = validateAnswers(formEntrySession.getFormEntryController(),
                            formEntrySession.getFormEntryModel(),
                            answerQuestionBean.getAnswersToValidate(),
                            formEntrySession.getSkipValidation());
                    responseBean.setErrors(errors);
                }
        );

        updateSession(formEntrySession);

        categoryTimingHelper.timed(
                Constants.TimingCategories.COMPILE_RESPONSE,
                () -> {
                    responseBean.setTitle(serializableFormSession.getTitle());
                    responseBean.setSequenceId(serializableFormSession.getVersion());
                    responseBean.setInstanceXml(new InstanceXmlBean(serializableFormSession.getInstanceXml()));
                }
        );

        return responseBean;
    }

    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean,
                                         @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken, HttpServletRequest request) throws Exception {
        FormSubmissionContext context = getFormProcessingContext(request, submitRequestBean);

        Stream<CheckedSupplier<SubmitResponseBean>> processingSteps = Stream.of(
                () -> validateAnswers(context),
                () -> processFormXml(context)
        );

        try {
            restoreFactory.setAutoCommit(false);

            Optional<SubmitResponseBean> error = processingSteps
                    .map((step) -> checkResponse(request, step))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();

            if (error.isPresent()) {
                return error.get();
            }

            try {
                String response = submitService.submitForm(
                        context.getFormEntrySession().getInstanceXml(),
                        context.getFormEntrySession().getPostUrl()
                );
                parseSubmitResponseMessage(response, context.getResponse());
            } catch (HttpClientErrorException.TooManyRequests e) {
                return context.error(Constants.SUBMIT_RESPONSE_TOO_MANY_REQUESTS);
            } catch (HttpClientErrorException e) {
                return getErrorResponse(
                        request, "error",
                        String.format("Form submission failed with error response: %s, %s, %s",
                                e.getMessage(), e.getResponseBodyAsString(), e.getResponseHeaders()),
                        e);
            }

            // Only delete session immediately after successful submit
            deleteSession(submitRequestBean.getSessionId());
            restoreFactory.commit();
        } finally {
            // If autoCommit hasn't been reset to `true` by the commit() call then an error occurred
            if (!restoreFactory.getAutoCommit()) {
                // rollback sets autoCommit back to `true`
                restoreFactory.rollback();
            }
        }

        updateVolatility(context.getFormEntrySession());

        performSync(context.getFormEntrySession());

        context.getResponse().setNextScreen(
                doEndOfFormNav(context.getFormEntrySession(), context.getMetricsTags(), context.getResponse())
        );
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

    private Optional<SubmitResponseBean> checkResponse(HttpServletRequest request, CheckedSupplier<SubmitResponseBean> supplier) {
        SubmitResponseBean response = null;
        try {
            response = supplier.get();
        } catch (Exception e) {
            response = getErrorResponse(
                    request, Constants.ANSWER_RESPONSE_STATUS_NEGATIVE,
                    e.getMessage(), e);
        }
        if (response.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE)) {
            return Optional.empty();  // continue processing
        }
        return Optional.of(response);
    }

    private SubmitResponseBean validateAnswers(FormSubmissionContext context) {
        categoryTimingHelper.timed(
                Constants.TimingCategories.VALIDATE_SUBMISSION,
                () -> validateSubmitAnswers(context),
                context.getMetricsTags()
        );
        if (!context.getResponse().getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE)
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

    private SubmitResponseBean processFormXml(FormSubmissionContext context) throws Exception {
        try {
            processXmlInner(context);
        } catch (InvalidCaseGraphException e) {
            return getErrorResponse(
                    context.getHttpRequest(), Constants.SUBMIT_RESPONSE_CASE_CYCLE_ERROR,
                    "Form submission failed due to a cyclic case relationship. " +
                            "Please contact the support desk to help resolve this issue.", e);
        } catch (Exception e) {
            return getErrorResponse(
                    context.getHttpRequest(), Constants.ANSWER_RESPONSE_STATUS_NEGATIVE,
                    e.getMessage(), e);
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

    private Object doEndOfFormNav(FormSession formEntrySession, Map<String, String> extras, SubmitResponseBean submitResponseBean) {
        return categoryTimingHelper.timed(
            Constants.TimingCategories.END_OF_FORM_NAV,
            () -> {
                if (formEntrySession.getMenuSessionId() != null &&
                        !("").equals(formEntrySession.getMenuSessionId().trim())) {
                    return doEndOfFormNav(menuSessionService.getSessionById(formEntrySession.getMenuSessionId()));
                }
                return null;
            },
            extras
        );
    }

    private void performSync(FormSession formEntrySession) throws Exception {
        boolean suppressAutosync = formEntrySession.getSuppressAutosync();

        if (storageFactory.getPropertyManager().isSyncAfterFormEnabled() && !suppressAutosync) {
            //If configured to do so, do a sync with server now to ensure dats is up to date.
            //Need to do before end of form nav triggers, since the new data might change the
            //validity of the form

            boolean skipFixtures = storageFactory.getPropertyManager().skipFixturesAfterSubmit();
            restoreFactory.performTimedSync(true, skipFixtures, false);
        }
    }

    private void updateVolatility(FormSession formEntrySession) {
        FormVolatilityRecord volatilityRecord = formEntrySession.getSessionVolatilityRecord();
        if (volatilityCache != null && volatilityRecord != null) {
            FormVolatilityRecord existingRecord = volatilityCache.get(volatilityRecord.getKey());
            if (existingRecord != null && existingRecord.matchesUser(formEntrySession)) {
                volatilityRecord = existingRecord;
            }
            volatilityRecord.updateFormSubmitted(formEntrySession);
            volatilityRecord.write(volatilityCache);
        }
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
        HashMap<String, ErrorBean> errors = validateAnswers(
                context.getFormEntrySession().getFormEntryController(),
                context.getFormEntrySession().getFormEntryModel(),
                context.getRequest().getAnswers(),
                context.getFormEntrySession().getSkipValidation());
        if (errors.size() > 0) {
            context.error(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE, errors);
        }
    }

    // Iterate over all answers and attempt to save them to check for validity.
    private HashMap<String, ErrorBean> validateAnswers(FormEntryController formEntryController,
                                                       FormEntryModel formEntryModel,
                                                       @Nullable Map<String, Object> answers,
                                                       boolean skipValidation) {
        HashMap<String, ErrorBean> errors = new HashMap<>();
        if (answers != null) {
            for (String key : answers.keySet()) {
                int questionType = JsonActionUtils.getQuestionType(formEntryModel, key, formEntryModel.getForm());
                if (!(questionType == FormEntryController.EVENT_QUESTION)) {
                    continue;
                }
                String answer = answers.get(key) == null ? null : answers.get(key).toString();
                JSONObject answerResult =
                        JsonActionUtils.questionAnswerToJson(formEntryController,
                                formEntryModel,
                                answer,
                                key,
                                false,
                                null,
                                skipValidation,
                                false);
                if (!answerResult.get(ApiConstants.RESPONSE_STATUS_KEY).equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE)) {
                    ErrorBean error = new ErrorBean();
                    error.setStatus(answerResult.get(ApiConstants.RESPONSE_STATUS_KEY).toString());
                    error.setType(answerResult.getString(ApiConstants.ERROR_TYPE_KEY));
                    errors.put(key, error);
                }
            }
        }
        return errors;
    }

    @RequestMapping(value = Constants.URL_NEW_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryResponseBean newRepeat(@RequestBody RepeatRequestBean newRepeatRequestBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(newRepeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession,
                restoreFactory, formSendCalloutHandler, storageFactory);
        JSONObject response = JsonActionUtils.descendRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                newRepeatRequestBean.getRepeatIndex());
        updateSession(formEntrySession);
        FormEntryResponseBean responseBean = mapper.readValue(response.toString(), FormEntryResponseBean.class);
        responseBean.setTitle(serializableFormSession.getTitle());
        responseBean.setInstanceXml(new InstanceXmlBean(serializableFormSession.getInstanceXml()));
        log.info("New response: " + responseBean);
        return responseBean;
    }

    @RequestMapping(value = Constants.URL_DELETE_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryResponseBean deleteRepeat(@RequestBody RepeatRequestBean deleteRepeatRequestBean,
                                              @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(deleteRepeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory);
        JSONObject response = JsonActionUtils.deleteRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                deleteRepeatRequestBean.getRepeatIndex(), deleteRepeatRequestBean.getFormIndex());
        updateSession(formEntrySession);
        FormEntryResponseBean responseBean = mapper.readValue(response.toString(), FormEntryResponseBean.class);
        responseBean.setTitle(serializableFormSession.getTitle());
        responseBean.setInstanceXml(new InstanceXmlBean(serializableFormSession.getInstanceXml()));
        return responseBean;
    }

    @RequestMapping(value = Constants.URL_NEXT_INDEX, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryNavigationResponseBean getNext(@RequestBody SessionRequestBean requestBean,
                                                   @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(requestBean.getSessionId());
        FormSession formSession = new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory);
        formSession.stepToNextIndex();
        FormEntryNavigationResponseBean responseBean = formSession.getFormNavigation();
        updateSession(formSession);
        return responseBean;
    }

    @RequestMapping(value = Constants.URL_NEXT, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryNavigationResponseBean getNextSms(@RequestBody SessionRequestBean requestBean,
                                                      @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(requestBean.getSessionId());
        FormSession formSession = new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory);
        FormEntryNavigationResponseBean responseBean = formSession.getNextFormNavigation();
        updateSession(formSession);
        return responseBean;
    }

    @RequestMapping(value = Constants.URL_PREV_INDEX, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryNavigationResponseBean getPrevious(@RequestBody SessionRequestBean requestBean,
                                                       @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(requestBean.getSessionId());
        FormSession formSession = new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory);
        formSession.stepToPreviousIndex();
        FormEntryNavigationResponseBean responseBean = formSession.getFormNavigation();
        updateSession(formSession);
        return responseBean;
    }

    @RequestMapping(value = Constants.URL_GET_INSTANCE, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public GetInstanceResponseBean getRawInstance(@RequestBody SessionRequestBean requestBean,
                                                  @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(requestBean.getSessionId());
        FormSession formSession = new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory);
        return new GetInstanceResponseBean(formSession);
    }


    @RequestMapping(value = Constants.URL_CURRENT, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryNavigationResponseBean getCurrent(@RequestBody SessionRequestBean requestBean,
                                                      @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        org.commcare.formplayer.objects.SerializableFormSession serializableFormSession = formSessionService.getSessionById(requestBean.getSessionId());
        FormSession formSession = new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory);
        FormEntryNavigationResponseBean responseBean = formSession.getFormNavigation();
        updateSession(formSession);
        return responseBean;
    }

    private void updateSession(FormSession formEntrySession) throws Exception {
        categoryTimingHelper.timed(
                Constants.TimingCategories.UPDATE_SESSION,
                () -> formSessionService.saveSession(formEntrySession.serialize())
        );
    }
}
