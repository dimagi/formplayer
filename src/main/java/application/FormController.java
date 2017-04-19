package application;

import annotations.UserLock;
import annotations.UserRestore;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.*;
import beans.menus.ErrorBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import engine.FormplayerTransactionParserFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import api.json.JsonActionUtils;
import api.process.FormRecordProcessorHelper;
import api.util.ApiConstants;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xml.util.InvalidStructureException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repo.FormSessionRepo;
import repo.SerializableMenuSession;
import services.SubmitService;
import services.XFormService;
import session.FormSession;
import session.MenuSession;
import util.Constants;
import util.PropertyUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller class (API endpoint) containing all form entry logic. This includes
 * opening a new form, question answering, and form submission.
 */
@Api(value = "Form Controller", description = "Operations for navigating CommCare Forms")
@RestController
@EnableAutoConfiguration
public class FormController extends AbstractBaseController{

    @Autowired
    private XFormService xFormService;

    @Autowired
    private SubmitService submitService;

    @Autowired
    @Qualifier(value = "migrated")
    protected FormSessionRepo migratedFormSessionRepo;

    @Value("${commcarehq.host}")
    private String host;

    private final Log log = LogFactory.getLog(FormController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @ApiOperation(value = "Start a new form entry session")
    @RequestMapping(value = Constants.URL_NEW_SESSION, method = RequestMethod.POST)
    @UserLock
    @UserRestore
        public NewFormResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        String postUrl = host + newSessionBean.getPostUrl();
        return newFormResponseFactory.getResponse(newSessionBean,
                postUrl,
                new DjangoAuth(authToken));
    }

    @ApiOperation(value = "Answer the question at the given index")
    @RequestMapping(value = Constants.URL_ANSWER_QUESTION, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    public FormEntryResponseBean answerQuestion(@RequestBody AnswerQuestionRequestBean answerQuestionBean,
                                                @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession session = formSessionRepo.findOneWrapped(answerQuestionBean.getSessionId());
        FormSession formEntrySession = new FormSession(session, restoreFactory);
        JSONObject resp = formEntrySession.answerQuestionToJSON(answerQuestionBean.getAnswer(),
                answerQuestionBean.getFormIndex());
        updateSession(formEntrySession, session);
        FormEntryResponseBean responseBean = mapper.readValue(resp.toString(), FormEntryResponseBean.class);
        responseBean.setTitle(formEntrySession.getTitle());
        responseBean.setSequenceId(formEntrySession.getSequenceId());
        responseBean.setInstanceXml(new InstanceXmlBean(formEntrySession));
        return responseBean;
    }

    @ApiOperation(value = "Submit the current form")
    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean,
                                             @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {

        HqAuth auth = getAuthHeaders(
                submitRequestBean.getDomain(),
                submitRequestBean.getUsername(),
                authToken
        );
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(submitRequestBean.getSessionId());
        storageFactory.configure(serializableFormSession.getUsername(),
                serializableFormSession.getDomain(),
                serializableFormSession.getAppId(),
                serializableFormSession.getAsUser()
        );

        FormSession formEntrySession = new FormSession(serializableFormSession, restoreFactory);
        SubmitResponseBean submitResponseBean;

        if (serializableFormSession.getOneQuestionPerScreen()) {
            // todo separate workflow for one question per screen
            submitResponseBean = new SubmitResponseBean(Constants.SYNC_RESPONSE_STATUS_POSITIVE);
        } else {
            submitResponseBean = validateSubmitAnswers(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                submitRequestBean.getAnswers());
        }

        if (!submitResponseBean.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE)
                || !submitRequestBean.isPrevalidated()) {
            submitResponseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
        } else {
            try {
                FormRecordProcessorHelper.processXML(new FormplayerTransactionParserFactory(restoreFactory.getSqlSandbox(),
                                PropertyUtils.isBulkPerformanceEnabled()),
                        formEntrySession.submitGetXml());
            } catch(InvalidStructureException e) {
                submitResponseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
                submitResponseBean.setNotification(new NotificationMessageBean(e.getMessage(), true));
                log.error("Submission failed with structure exception " + e);
                return submitResponseBean;
            }

            ResponseEntity<String> submitResponse = submitService.submitForm(
                    formEntrySession.getInstanceXml(),
                    formEntrySession.getPostUrl(),
                    auth
            );

            if (!submitResponse.getStatusCode().is2xxSuccessful()) {
                submitResponseBean.setStatus("error");
                submitResponseBean.setNotification(new NotificationMessageBean(
                        "Form submission failed with error response" + submitResponse, true));
                log.info("Submit response bean: " + submitResponseBean);
                return submitResponseBean;
            }

            if (formEntrySession.getMenuSessionId() != null &&
                    !("").equals(formEntrySession.getMenuSessionId().trim())) {
                Object nav = doEndOfFormNav(menuSessionRepo.findOneWrapped(formEntrySession.getMenuSessionId()), auth);
                if (nav != null) {
                    submitResponseBean.setNextScreen(nav);
                }
            }
            deleteSession(submitRequestBean.getSessionId());
        }
        return submitResponseBean;
    }

    protected void deleteSession(String id) {
        formSessionRepo.delete(id);
        migratedFormSessionRepo.delete(id);
    }

    private Object doEndOfFormNav(SerializableMenuSession serializedSession, HqAuth auth) throws Exception {
        log.info("End of form navigation with serialized menu session: " + serializedSession);
        MenuSession menuSession = new MenuSession(serializedSession, installService, restoreFactory, auth, host);
        return resolveFormGetNext(menuSession);
    }

    /**
     * Iterate over all answers and attempt to save them to check for validity.
     * Submit the complete XML instance to HQ if valid.
     */
    private SubmitResponseBean validateSubmitAnswers(FormEntryController formEntryController,
                                       FormEntryModel formEntryModel,
                                       Map<String, Object> answers) {
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
                            null);
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

    @ApiOperation(value = "Expand the repeat at the given index")
    @RequestMapping(value = Constants.URL_NEW_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    public FormEntryResponseBean newRepeat(@RequestBody RepeatRequestBean newRepeatRequestBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(newRepeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession, restoreFactory);
        JSONObject response = JsonActionUtils.descendRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                newRepeatRequestBean.getRepeatIndex());
        updateSession(formEntrySession, serializableFormSession);
        FormEntryResponseBean responseBean = mapper.readValue(response.toString(), FormEntryResponseBean.class);
        responseBean.setTitle(formEntrySession.getTitle());
        responseBean.setInstanceXml(new InstanceXmlBean(formEntrySession));
        log.info("New response: " + responseBean);
        return responseBean;
    }

    @ApiOperation(value = "Delete the repeat at the given index")
    @RequestMapping(value = Constants.URL_DELETE_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    @UserRestore
    public FormEntryResponseBean deleteRepeat(@RequestBody RepeatRequestBean deleteRepeatRequestBean,
                                              @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(deleteRepeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession, restoreFactory);
        JSONObject response = JsonActionUtils.deleteRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                deleteRepeatRequestBean.getRepeatIndex(), deleteRepeatRequestBean.getFormIndex());
        updateSession(formEntrySession, serializableFormSession);
        FormEntryResponseBean responseBean = mapper.readValue(response.toString(), FormEntryResponseBean.class);
        responseBean.setTitle(formEntrySession.getTitle());
        responseBean.setInstanceXml(new InstanceXmlBean(formEntrySession));
        return responseBean;
    }

    @ApiOperation(value = "Get the questions for the next index in OQPS mode")
    @RequestMapping(value = Constants.URL_NEXT_INDEX, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    public FormEntryNavigationResponseBean getNext(@RequestBody SessionRequestBean requestBean,
                                                   @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(requestBean.getSessionId());
        FormSession formSession = new FormSession(serializableFormSession, restoreFactory);
        formSession.stepToNextIndex();
        FormEntryNavigationResponseBean responseBean = formSession.getFormNavigation();
        updateSession(formSession, serializableFormSession);
        return responseBean;
    }

    @ApiOperation(value = "Get the questios for the previous index in OQPS mode")
    @RequestMapping(value = Constants.URL_PREV_INDEX, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    public FormEntryNavigationResponseBean getPrevious(@RequestBody SessionRequestBean requestBean,
                                                       @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(requestBean.getSessionId());
        FormSession formSession = new FormSession(serializableFormSession, restoreFactory);
        formSession.stepToPreviousIndex();
        FormEntryNavigationResponseBean responseBean = formSession.getFormNavigation();
        updateSession(formSession, serializableFormSession);
        return responseBean;
    }


    private void updateSession(FormSession formEntrySession, SerializableFormSession serialSession) throws IOException {
        formEntrySession.setSequenceId(formEntrySession.getSequenceId() + 1);
        serialSession.setInstanceXml(formEntrySession.getInstanceXml());
        serialSession.setSequenceId(formEntrySession.getSequenceId());
        serialSession.setCurrentIndex(formEntrySession.getCurrentIndex());
        formSessionRepo.save(serialSession);
    }
}
