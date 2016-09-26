package application;

import auth.DjangoAuth;
import auth.HqAuth;
import beans.*;
import beans.menus.ErrorBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.json.JsonActionUtils;
import org.commcare.api.process.FormRecordProcessorHelper;
import org.commcare.api.util.ApiConstants;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repo.SerializableMenuSession;
import services.NewFormResponseFactory;
import services.SubmitService;
import services.XFormService;
import session.FormSession;
import session.MenuSession;
import util.Constants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

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
    private NewFormResponseFactory newFormResponseFactory;

    @Value("${commcarehq.host}")
    private String host;

    private final Log log = LogFactory.getLog(FormController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @ApiOperation(value = "Start a new form entry session")
    @RequestMapping(value = Constants.URL_NEW_SESSION , method = RequestMethod.POST)
    public NewFormResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("New form requests with bean: " + newSessionBean + " sessionId :" + authToken);
        Lock lock = getLockAndBlock(newSessionBean.getSessionData().getUsername());
        try {
            String postUrl = host + newSessionBean.getPostUrl();
            NewFormResponse newSessionResponse = newFormResponseFactory.getResponse(newSessionBean,
                    postUrl,
                    new DjangoAuth(authToken));
            log.info("Return new session response: " + newSessionResponse);
            return newSessionResponse;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Open an incomplete form session")
    @RequestMapping(value = Constants.URL_INCOMPLETE_SESSION , method = RequestMethod.POST)
    public NewFormResponse openIncompleteForm(@RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
                                              @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Incomplete session request with bean: " + incompleteSessionRequestBean + " sessionId :" + authToken);
        SerializableFormSession session = formSessionRepo.findOneWrapped(incompleteSessionRequestBean.getSessionId());
        Lock lock = getLockAndBlock(session.getUsername());
        try {
            NewFormResponse response = newFormResponseFactory.getResponse(session);
            log.info("Return incomplete session response: " + response);
            return response;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Answer the question at the given index")
    @RequestMapping(value = Constants.URL_ANSWER_QUESTION, method = RequestMethod.POST)
    public FormEntryResponseBean answerQuestion(@RequestBody AnswerQuestionRequestBean answerQuestionBean) throws Exception {
        log.info("Answer question with bean: " + answerQuestionBean);
        SerializableFormSession session = formSessionRepo.findOneWrapped(answerQuestionBean.getSessionId());
        Lock lock = getLockAndBlock(session.getUsername());
        try {
            FormSession formEntrySession = new FormSession(session);
            FormEntryController entryController = formEntrySession.getFormEntryController();

            if (session.getOneQuestionPerScreen()) {
                entryController.setOneQuestionPerScreen(session.getOneQuestionPerScreen());
                entryController.setCurrentIndex(JsonActionUtils.indexFromString(
                    "" + session.getCurrentIndex(), formEntrySession.getFormDef())
                );
            }

            JSONObject resp = JsonActionUtils.questionAnswerToJson(entryController,
                    formEntrySession.getFormEntryModel(),
                    answerQuestionBean.getAnswer() != null ? answerQuestionBean.getAnswer().toString() : null,
                    answerQuestionBean.getFormIndex());
            updateSession(formEntrySession, session);
            FormEntryResponseBean responseBean = mapper.readValue(resp.toString(), FormEntryResponseBean.class);
            responseBean.setTitle(formEntrySession.getTitle());
            responseBean.setSequenceId(formEntrySession.getSequenceId());
            responseBean.setInstanceXml(new InstanceXmlBean(formEntrySession));
            log.info("Answer response: " + responseBean);
            return responseBean;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Get the current question (deprecated)")
    @RequestMapping(value = Constants.URL_CURRENT, method = RequestMethod.GET)
    @ResponseBody
    public FormEntryResponseBean getCurrent(@RequestBody CurrentRequestBean currentRequestBean) throws Exception {
        log.info("Current request: " + currentRequestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(currentRequestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formEntrySession = new FormSession(serializableFormSession);
            JSONObject resp = JsonActionUtils.getCurrentJson(formEntrySession.getFormEntryController(), formEntrySession.getFormEntryModel());
            FormEntryResponseBean responseBean = mapper.readValue(resp.toString(), FormEntryResponseBean.class);
            log.info("Current response: " + responseBean);
            return responseBean;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Submit the current form")
    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean,
                                             @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Submit form with bean: " + submitRequestBean);

        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(submitRequestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formEntrySession = new FormSession(serializableFormSession);

            log.info("Submitting form entry session has menuId: " + formEntrySession.getMenuSessionId());

            SubmitResponseBean submitResponseBean = validateSubmitAnswers(formEntrySession.getFormEntryController(),
                    formEntrySession.getFormEntryModel(),
                    submitRequestBean.getAnswers());

            if (!submitResponseBean.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE)
                    || !submitRequestBean.isPrevalidated()) {
                submitResponseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
            } else {
                FormRecordProcessorHelper.processXML(formEntrySession.getSandbox(), formEntrySession.submitGetXml());

                ResponseEntity<String> submitResponse =
                        submitService.submitForm(formEntrySession.getInstanceXml(),
                                formEntrySession.getPostUrl(),
                                new DjangoAuth(authToken));

                if (!submitResponse.getStatusCode().is2xxSuccessful()) {
                    submitResponseBean.setStatus("error");
                    submitResponseBean.setNotification(new NotificationMessageBean(
                            "Form submission failed with error response" + submitResponse, true));
                    log.info("Submit response bean: " + submitResponseBean);
                    return submitResponseBean;
                }
                if (formEntrySession.getMenuSessionId() != null &&
                        !("").equals(formEntrySession.getMenuSessionId().trim())) {
                    Object nav = doEndOfFormNav(menuSessionRepo.findOne(formEntrySession.getMenuSessionId()), new DjangoAuth(authToken));
                    if (nav != null) {
                        submitResponseBean.setNextScreen(nav);
                    }
                }
                formSessionRepo.delete(submitRequestBean.getSessionId());

            }
            log.info("Submit response bean: " + submitResponseBean);
            return submitResponseBean;
        } finally {
            lock.unlock();
        }
    }

    private Object doEndOfFormNav(SerializableMenuSession serializedSession, HqAuth auth) throws Exception {
        log.info("End of form navigation with serialized menu session: " + serializedSession);
        MenuSession menuSession = new MenuSession(serializedSession, installService, restoreService, auth, host);
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
                            key);
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

    @ApiOperation(value = "Get the current instance XML")
    @RequestMapping(value = Constants.URL_GET_INSTANCE, method = RequestMethod.POST)
    @ResponseBody
    public InstanceXmlBean getInstance(@RequestBody GetInstanceRequestBean getInstanceRequestBean) throws Exception {
        log.info("Get instance request: " + getInstanceRequestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(getInstanceRequestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formEntrySession = new FormSession(serializableFormSession);
            InstanceXmlBean instanceXmlBean = new InstanceXmlBean(formEntrySession);
            log.info("Get instance response: " + instanceXmlBean);
            return instanceXmlBean;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Evaluate the given XPath under the current context")
    @RequestMapping(value = Constants.URL_EVALUATE_XPATH, method = RequestMethod.POST)
    @ResponseBody
    public EvaluateXPathResponseBean evaluateXpath(@RequestBody EvaluateXPathRequestBean evaluateXPathRequestBean) throws Exception {
        log.info("Evaluate XPath Request: " + evaluateXPathRequestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(evaluateXPathRequestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formEntrySession = new FormSession(serializableFormSession);
            EvaluateXPathResponseBean evaluateXPathResponseBean =
                    new EvaluateXPathResponseBean(formEntrySession, evaluateXPathRequestBean.getXpath());
            log.info("Evaluate XPath Response: " + evaluateXPathResponseBean);
            return evaluateXPathResponseBean;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Expand the repeat at the given index")
    @RequestMapping(value = Constants.URL_NEW_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    public FormEntryResponseBean newRepeat(@RequestBody RepeatRequestBean newRepeatRequestBean) throws Exception {
        log.info("New repeat: " + newRepeatRequestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(newRepeatRequestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formEntrySession = new FormSession(serializableFormSession);
            JSONObject response = JsonActionUtils.descendRepeatToJson(formEntrySession.getFormEntryController(),
                    formEntrySession.getFormEntryModel(),
                    newRepeatRequestBean.getRepeatIndex());
            updateSession(formEntrySession, serializableFormSession);
            FormEntryResponseBean responseBean = mapper.readValue(response.toString(), FormEntryResponseBean.class);
            responseBean.setTitle(formEntrySession.getTitle());
            responseBean.setInstanceXml(new InstanceXmlBean(formEntrySession));
            log.info("New response: " + responseBean);
            return responseBean;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Delete the repeat at the given index")
    @RequestMapping(value = Constants.URL_DELETE_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    public FormEntryResponseBean deleteRepeat(@RequestBody RepeatRequestBean deleteRepeatRequestBean) throws Exception {
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(deleteRepeatRequestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formEntrySession = new FormSession(serializableFormSession);
            JSONObject response = JsonActionUtils.deleteRepeatToJson(formEntrySession.getFormEntryController(),
                    formEntrySession.getFormEntryModel(),
                    deleteRepeatRequestBean.getRepeatIndex(), deleteRepeatRequestBean.getFormIndex());
            updateSession(formEntrySession, serializableFormSession);
            FormEntryResponseBean responseBean = mapper.readValue(response.toString(), FormEntryResponseBean.class);
            responseBean.setTitle(formEntrySession.getTitle());
            responseBean.setInstanceXml(new InstanceXmlBean(formEntrySession));
            log.info("Delete repeat response: " + responseBean);
            return responseBean;
        } finally {
            lock.unlock();
        }
    }

    // Functions pertaining to One Question Per Screen (OQPS) mode

    @ApiOperation(value = "Get the question for the current index")
    @RequestMapping(value = Constants.URL_QUESTIONS_FOR_INDEX, method = RequestMethod.POST)
    @ResponseBody
    public FormEntryResponseBean jumpToIndex(@RequestBody JumpToIndexRequestBean requestBean) throws Exception {
        log.info("Jump to index request: " + requestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(requestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formSession = new FormSession(serializableFormSession);
            formSession.setCurrentIndex(requestBean.getFormIndex());
            JSONObject resp = JsonActionUtils.getCurrentJson(formSession.getFormEntryController(),
                    formSession.getFormEntryModel(),
                    formSession.getCurrentIndex());
            updateSession(formSession, serializableFormSession);
            FormEntryResponseBean responseBean = mapper.readValue(resp.toString(), FormEntryResponseBean.class);
            log.info("Jump to index response: " + responseBean);
            return responseBean;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Get the questions for the next index in OQPS mode")
    @RequestMapping(value = Constants.URL_NEXT_INDEX, method = RequestMethod.POST)
    @ResponseBody
    public FormEntryResponseBean getNext(@RequestBody SessionRequestBean requestBean) throws Exception {
        log.info("Get next questions for: " + requestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(requestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formSession = new FormSession(serializableFormSession);
            formSession.stepToNextIndex();
            JSONObject resp = JsonActionUtils.getCurrentJson(formSession.getFormEntryController(),
                    formSession.getFormEntryModel(),
                    formSession.getCurrentIndex());
            updateSession(formSession, serializableFormSession);
            FormEntryResponseBean responseBean = mapper.readValue(resp.toString(), FormEntryResponseBean.class);
            log.info("Next index response: " + responseBean);
            return responseBean;
        } finally {
            lock.unlock();
        }
    }

    @ApiOperation(value = "Get the questios for the previous index in OQPS mode")
    @RequestMapping(value = Constants.URL_PREV_INDEX, method = RequestMethod.POST)
    @ResponseBody
    public FormEntryResponseBean getPrevious(@RequestBody SessionRequestBean requestBean) throws Exception {
        log.info("Get previous questions for: " + requestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(requestBean.getSessionId());
        Lock lock = getLockAndBlock(serializableFormSession.getUsername());
        try {
            FormSession formSession = new FormSession(serializableFormSession);
            formSession.stepToPreviousIndex();
            JSONObject resp = JsonActionUtils.getCurrentJson(formSession.getFormEntryController(),
                    formSession.getFormEntryModel(),
                    formSession.getCurrentIndex());
            updateSession(formSession, serializableFormSession);
            FormEntryResponseBean responseBean = mapper.readValue(resp.toString(), FormEntryResponseBean.class);
            log.info("Previous index response " + responseBean);
            return responseBean;
        } finally {
            lock.unlock();
        }
    }


    private void updateSession(FormSession formEntrySession, SerializableFormSession serialSession) throws IOException {
        formEntrySession.setSequenceId(formEntrySession.getSequenceId() + 1);
        serialSession.setInstanceXml(formEntrySession.getInstanceXml());
        serialSession.setSequenceId(formEntrySession.getSequenceId());
        serialSession.setCurrentIndex(formEntrySession.getCurrentIndex());
        formSessionRepo.save(serialSession);
    }
}