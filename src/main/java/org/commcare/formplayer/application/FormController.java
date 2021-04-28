package org.commcare.formplayer.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.sentry.Sentry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.util.InvalidCaseGraphException;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xml.util.InvalidStructureException;
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
import org.commcare.formplayer.repo.SerializableMenuSession;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.util.SimpleTimer;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Controller class (API endpoint) containing all form entry logic. This includes
 * opening a new form, question answering, and form submission.
 */
@RestController
@EnableAutoConfiguration
public class FormController extends AbstractBaseController{

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

    @Resource(name="redisVolatilityDict")
    private ValueOperations<String, FormVolatilityRecord> volatilityCache;

    @Value("${commcarehq.host}")
    private String host;

    private final Log log = LogFactory.getLog(FormController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @RequestMapping(value = Constants.URL_NEW_SESSION, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    public NewFormResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean,
                                           @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {
        String postUrl = host + newSessionBean.getPostUrl();
        return newFormResponseFactory.getResponse(newSessionBean, postUrl);
    }

    @RequestMapping(value = Constants.URL_CHANGE_LANGUAGE, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryResponseBean changeLocale(@RequestBody ChangeLocaleRequestBean changeLocaleBean,
                                                @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {
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
                                                @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {

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
                                         @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken, HttpServletRequest request) throws Exception {
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(submitRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession, restoreFactory, formSendCalloutHandler, storageFactory);

        // add tags for future datadog/sentry requests
        datadog.addRequestScopedTag(Constants.FORM_NAME_TAG, serializableFormSession.getTitle());
        Sentry.setTag(Constants.FORM_NAME_TAG, serializableFormSession.getTitle());

        // package additional args to pass to category timing helper
        Map<String, String> extras = new HashMap<String, String>();
        extras.put(Constants.DOMAIN_TAG, submitRequestBean.getDomain());

        SubmitResponseBean submitResponseBean;
        SimpleTimer validationTimer = new SimpleTimer();
        validationTimer.start();
        submitResponseBean = validateSubmitAnswers(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                submitRequestBean.getAnswers(),
                formEntrySession.getSkipValidation());
        validationTimer.end();
        categoryTimingHelper.recordCategoryTiming(validationTimer, Constants.TimingCategories.VALIDATE_SUBMISSION, null, extras);

        FormVolatilityRecord volatilityRecord = formEntrySession.getSessionVolatilityRecord();

        if (!submitResponseBean.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE)
                || !submitRequestBean.isPrevalidated()) {
            submitResponseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
        } else {
            try {
                restoreFactory.setAutoCommit(false);

                SimpleTimer purgeCasesTimer = null;

                try {
                    purgeCasesTimer = FormRecordProcessorHelper.processXML(
                            new FormplayerTransactionParserFactory(
                                    restoreFactory.getSqlSandbox(),
                                    storageFactory.getPropertyManager().isBulkPerformanceEnabled()
                            ),
                            formEntrySession.submitGetXml(),
                            storageFactory.getPropertyManager().isAutoPurgeEnabled()
                    ).getPurgeCasesTimer();
                } catch (InvalidCaseGraphException e) {
                    submitResponseBean.setStatus(Constants.SUBMIT_RESPONSE_CASE_CYCLE_ERROR);
                    NotificationMessage notification = new NotificationMessage(
                            "Form submission failed due to a cyclic case relationship. Please contact the support desk to help resolve this issue.",
                            true,
                            NotificationMessage.Tag.submit);
                    submitResponseBean.setNotification(notification);
                    logNotification(notification, request);
                    log.error("Submission failed with structure exception " + e);
                    return submitResponseBean;
                }

                categoryTimingHelper.recordCategoryTiming(purgeCasesTimer, Constants.TimingCategories.PURGE_CASES,
                        purgeCasesTimer.durationInMs() > 2 ?
                                "Purging cases took some time" : "Probably didn't have to purge cases", extras);

                String response;
                try {
                    response = submitService.submitForm(
                            formEntrySession.getInstanceXml(),
                            formEntrySession.getPostUrl()
                    );
                } catch (HttpClientErrorException.TooManyRequests e) {
                    submitResponseBean.setStatus(Constants.SUBMIT_RESPONSE_TOO_MANY_REQUESTS);
                    return submitResponseBean;
                } catch (HttpClientErrorException e) {
                    submitResponseBean.setStatus("error");
                    NotificationMessage notification = new NotificationMessage(
                            String.format("Form submission failed with error response: %s, %s, %s",
                                    e.getMessage(), e.getResponseBodyAsString(), e.getResponseHeaders()),
                            true, NotificationMessage.Tag.submit);
                    submitResponseBean.setNotification(notification);
                    logNotification(notification, request);
                    log.error("Submit response bean: " + submitResponseBean);
                    return submitResponseBean;
                }

                parseSubmitResponseMessage(response, submitResponseBean);

                // Only delete session immediately after successful submit
                deleteSession(submitRequestBean.getSessionId());
                restoreFactory.commit();

            }
            catch (InvalidStructureException e) {
                submitResponseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
                NotificationMessage notification = new NotificationMessage(e.getMessage(), true, NotificationMessage.Tag.submit);
                submitResponseBean.setNotification(notification);
                logNotification(notification, request);
                log.error("Submission failed with structure exception " + e);
                return submitResponseBean;
            }
            finally {
                // If autoCommit hasn't been reset to `true` by the commit() call then an error occurred
                if (!restoreFactory.getAutoCommit()) {
                    // rollback sets autoCommit back to `true`
                    restoreFactory.rollback();
                }
            }

            if (volatilityCache != null &&  volatilityRecord != null) {
                FormVolatilityRecord existingRecord = volatilityCache.get(volatilityRecord.getKey());
                if (existingRecord != null && existingRecord.matchesUser(formEntrySession)) {
                    volatilityRecord = existingRecord;
                }
                volatilityRecord.updateFormSubmitted(formEntrySession);
                volatilityRecord.write(volatilityCache);
            }

            boolean suppressAutosync = formEntrySession.getSuppressAutosync();

            if (storageFactory.getPropertyManager().isSyncAfterFormEnabled() && !suppressAutosync) {
                //If configured to do so, do a sync with server now to ensure dats is up to date.
                //Need to do before end of form nav triggers, since the new data might change the
                //validity of the form

                boolean skipFixtures = storageFactory.getPropertyManager().skipFixturesAfterSubmit();
                restoreFactory.performTimedSync(true, skipFixtures, false);
            }

            SimpleTimer navTimer = new SimpleTimer();
            navTimer.start();
            if (formEntrySession.getMenuSessionId() != null &&
                    !("").equals(formEntrySession.getMenuSessionId().trim())) {
                Object nav = doEndOfFormNav(menuSessionRepo.findOneWrapped(formEntrySession.getMenuSessionId()));
                if (nav != null) {
                    submitResponseBean.setNextScreen(nav);
                }
            }
            navTimer.end();
            categoryTimingHelper.recordCategoryTiming(navTimer, Constants.TimingCategories.END_OF_FORM_NAV, null, extras);
        }
        return submitResponseBean;
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
    private SubmitResponseBean validateSubmitAnswers(FormEntryController formEntryController,
                                       FormEntryModel formEntryModel,
                                       Map<String, Object> answers,
                                       boolean skipValidation) {
        SubmitResponseBean submitResponseBean = new SubmitResponseBean(Constants.SYNC_RESPONSE_STATUS_POSITIVE);
        HashMap<String, ErrorBean> errors = new HashMap<>();
        for(String key: answers.keySet()){
            int questionType = JsonActionUtils.getQuestionType(formEntryModel, key, formEntryModel.getForm());
            if(!(questionType == FormEntryController.EVENT_QUESTION)){
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
            if(!answerResult.get(ApiConstants.RESPONSE_STATUS_KEY).equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE)) {
                submitResponseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
                ErrorBean error = new ErrorBean();
                error.setStatus(answerResult.get(ApiConstants.RESPONSE_STATUS_KEY).toString());
                error.setType(answerResult.getString(ApiConstants.ERROR_TYPE_KEY));
                errors.put(key, error);
            }
        }
        submitResponseBean.setErrors(errors);
        return submitResponseBean;
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
                                              @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {
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
                                                   @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {
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
                                                   @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {
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
                                                       @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {
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
                                                  @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {
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
                                                       @CookieValue(name=Constants.POSTGRES_DJANGO_SESSION_ID, required=false) String authToken) throws Exception {
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
