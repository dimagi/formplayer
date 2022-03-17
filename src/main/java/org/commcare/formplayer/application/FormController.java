package org.commcare.formplayer.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.annotations.ConfigureStorageFromSession;
import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.api.json.JsonActionUtils;
import org.commcare.formplayer.api.util.ApiConstants;
import org.commcare.formplayer.beans.*;
import org.commcare.formplayer.beans.menus.ErrorBean;
import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

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
        FormSession formEntrySession = getFormSession(serializableFormSession);
        formEntrySession.changeLocale(changeLocaleBean.getLocale());
        FormEntryResponseBean responseBean = formEntrySession.getCurrentJson();
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
                () -> getFormSession(serializableFormSession)
        );

        FormEntryResponseBean responseBean = categoryTimingHelper.timed(
                Constants.TimingCategories.PROCESS_ANSWER,
                () -> formEntrySession.answerQuestionToJson(
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

    // Iterate over all answers and attempt to save them to check for validity.
    public static HashMap<String, ErrorBean> validateAnswers(FormEntryController formEntryController,
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
        FormSession formEntrySession = getFormSession(serializableFormSession);
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
        FormSession formEntrySession = getFormSession(serializableFormSession);
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
        FormSession formSession = getFormSession(serializableFormSession);
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
        FormSession formSession = getFormSession(serializableFormSession);
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
        FormSession formSession = getFormSession(serializableFormSession);
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
        FormSession formSession = getFormSession(serializableFormSession);
        Boolean serializeAllData = !requestBean.getForSubmission();
        return new GetInstanceResponseBean(formSession, serializeAllData);
    }


    @RequestMapping(value = Constants.URL_CURRENT, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public FormEntryNavigationResponseBean getCurrent(@RequestBody SessionRequestBean requestBean,
                                                      @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        org.commcare.formplayer.objects.SerializableFormSession serializableFormSession = formSessionService.getSessionById(requestBean.getSessionId());
        FormSession formSession = getFormSession(serializableFormSession);
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
