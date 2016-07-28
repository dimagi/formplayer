package application;

import auth.DjangoAuth;
import beans.*;
import beans.menus.ErrorBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import hq.CaseAPIs;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repo.SessionRepo;
import requests.NewFormRequest;
import services.RestoreService;
import services.SubmitService;
import services.XFormService;
import session.FormSession;
import springfox.documentation.spring.web.json.Json;
import util.Constants;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by willpride on 1/12/16.
 */
@Api(value = "Form Controller", description = "Operations for navigating CommCare Forms")
@RestController
@EnableAutoConfiguration
public class FormController {

    @Autowired
    private SessionRepo sessionRepo;

    @Autowired
    private XFormService xFormService;

    @Autowired
    private RestoreService restoreService;

    @Autowired
    private SubmitService submitService;

    private final Log log = LogFactory.getLog(FormController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @ApiOperation(value = "Start a new form entry session")
    @RequestMapping(value = Constants.URL_NEW_SESSION , method = RequestMethod.POST)
    public NewFormSessionResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean,
                                                  @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("New form requests with bean: " + newSessionBean + " sessionId :" + authToken);
        NewFormRequest newFormRequest = new NewFormRequest(newSessionBean, sessionRepo, xFormService, restoreService, authToken);
        NewFormSessionResponse newSessionResponse = newFormRequest.getResponse();
        log.info("Return new session response: " + newSessionResponse);
        return newSessionResponse;
    }

    @ApiOperation(value = "Open an incomplete form session")
    @RequestMapping(value = Constants.URL_INCOMPLETE_SESSION , method = RequestMethod.POST)
    public NewFormSessionResponse openIncompleteForm(@RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
                                                  @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Incomplete session request with bean: " + incompleteSessionRequestBean + " sessionId :" + authToken);
        SerializableFormSession session = sessionRepo.findOne(incompleteSessionRequestBean.getSessionId());
        NewFormRequest newFormRequest = new NewFormRequest(session, restoreService, authToken);
        NewFormSessionResponse response = newFormRequest.getResponse();
        log.info("Return incomplete session response: " + response);
        return response;
    }

    @ApiOperation(value = "Answer the question at the given index")
    @RequestMapping(value = Constants.URL_ANSWER_QUESTION, method = RequestMethod.POST)
    public AnswerQuestionResponseBean answerQuestion(@RequestBody AnswerQuestionRequestBean answerQuestionBean) throws Exception {
        log.info("Answer question with bean: " + answerQuestionBean);
        SerializableFormSession session = sessionRepo.findOne(answerQuestionBean.getSessionId());
        log.info("Restored serialized session: " + session);
        FormSession formEntrySession = new FormSession(session);

        JSONObject resp = JsonActionUtils.questionAnswerToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                answerQuestionBean.getAnswer() != null? answerQuestionBean.getAnswer().toString() : null,
                answerQuestionBean.getFormIndex());

        updateSession(formEntrySession, session);

        AnswerQuestionResponseBean responseBean = mapper.readValue(resp.toString(), AnswerQuestionResponseBean.class);
        responseBean.setSequenceId(formEntrySession.getSequenceId() + 1);
        log.info("Answer response: " + responseBean);
        return responseBean;
    }

    @ApiOperation(value = "Get the current question (deprecated)")
    @RequestMapping(value = Constants.URL_CURRENT, method = RequestMethod.GET)
    @ResponseBody
    public CurrentResponseBean getCurrent(@RequestBody CurrentRequestBean currentRequestBean) throws Exception {
        log.info("Current request: " + currentRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.findOne(currentRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        CurrentResponseBean currentResponseBean = new CurrentResponseBean(formEntrySession);
        log.info("Current response: " + currentResponseBean);
        return currentResponseBean;
    }
    @ApiOperation(value = "Submit the current form")
    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean,
                                             @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Submit form with bean: " + submitRequestBean);

        SerializableFormSession serializableFormSession = sessionRepo.findOne(submitRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        FormRecordProcessorHelper.processXML(formEntrySession.getSandbox(), formEntrySession.submitGetXml());

        SubmitResponseBean submitResponseBean = validateSubmitAnswers(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                submitRequestBean.getAnswers());

        if(!submitResponseBean.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE)
                || !submitRequestBean.isPrevalidated()) {
            submitResponseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
        } else {

            ResponseEntity<String> submitResponse =
                    submitService.submitForm(formEntrySession.getInstanceXml(),
                            formEntrySession.getPostUrl(),
                            new DjangoAuth(authToken));

            if (submitResponse.getStatusCode().is2xxSuccessful()) {
                sessionRepo.delete(submitRequestBean.getSessionId());
            } else {
                submitResponseBean.setStatus("error");
                //TODO: need new way to communicate erros with submitting (vs. validation)
            }
        }
        log.info("Submit response bean: " + submitResponseBean);
        return submitResponseBean;
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
    public GetInstanceResponseBean getInstance(@RequestBody GetInstanceRequestBean getInstanceRequestBean) throws Exception {
        log.info("Get instance request: " + getInstanceRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.findOne(getInstanceRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        GetInstanceResponseBean getInstanceResponseBean = new GetInstanceResponseBean(formEntrySession);
        log.info("Get instance response: " + getInstanceResponseBean);
        return getInstanceResponseBean;
    }

    @ApiOperation(value = "Evaluate the given XPath under the current context")
    @RequestMapping(value = Constants.URL_EVALUATE_XPATH, method = RequestMethod.GET)
    @ResponseBody
    public EvaluateXPathResponseBean evaluateXpath(@RequestBody EvaluateXPathRequestBean evaluateXPathRequestBean) throws Exception {
        log.info("Evaluate XPath Request: " + evaluateXPathRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.findOne(evaluateXPathRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        EvaluateXPathResponseBean evaluateXPathResponseBean =
                new EvaluateXPathResponseBean(formEntrySession, evaluateXPathRequestBean.getXpath());
        log.info("Evaluate XPath Response: " + evaluateXPathResponseBean);
        return evaluateXPathResponseBean;
    }

    @ApiOperation(value = "Expand the repeat at the given index")
    @RequestMapping(value = Constants.URL_NEW_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    public RepeatResponseBean newRepeat(@RequestBody RepeatRequestBean newRepeatRequestBean) throws Exception {
        log.info("New repeat: " + newRepeatRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.findOne(newRepeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);

        JsonActionUtils.descendRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                newRepeatRequestBean.getRepeatIndex());

        updateSession(formEntrySession, serializableFormSession);

        JSONObject response = JsonActionUtils.getCurrentJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel());
        RepeatResponseBean repeatResponseBean = mapper.readValue(response.toString(), RepeatResponseBean.class);
        log.info("New response: " + repeatResponseBean);
        return repeatResponseBean;
    }

    @ApiOperation(value = "Delete the repeat at the given index")
    @RequestMapping(value = Constants.URL_DELETE_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    public RepeatResponseBean deleteRepeat(@RequestBody RepeatRequestBean repeatRequestBean) throws Exception {
        SerializableFormSession serializableFormSession = sessionRepo.findOne(repeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);

        JSONObject resp = JsonActionUtils.deleteRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                repeatRequestBean.getRepeatIndex(), repeatRequestBean.getFormIndex());

        updateSession(formEntrySession, serializableFormSession);

        return mapper.readValue(resp.toString(), RepeatResponseBean.class);
    }

    @ApiOperation(value = "Filter the user's casedb given a predicate expression")
    @RequestMapping(value = Constants.URL_FILTER_CASES, method = RequestMethod.GET)
    public CaseFilterResponseBean filterCasesHQ(@RequestBody CaseFilterRequestBean filterRequest) throws Exception {
        filterRequest.setRestoreService(restoreService);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        return new CaseFilterResponseBean(caseResponse);
    }

    @ApiOperation(value = "Fitler the user's casedb given a predicate expression returning all case data")
    @RequestMapping(value = Constants.URL_FILTER_CASES_FULL, method = RequestMethod.GET)
    public CaseFilterFullResponseBean filterCasesFull(@RequestBody CaseFilterRequestBean filterRequest) throws Exception {
        filterRequest.setRestoreService(restoreService);
        CaseBean[] caseResponse = CaseAPIs.filterCasesFull(filterRequest);
        return new CaseFilterFullResponseBean(caseResponse);
    }

    @ApiOperation(value = "Sync the user's database with the server")
    @RequestMapping(value = Constants.URL_SYNC_DB, method = RequestMethod.POST)
    public SyncDbResponseBean syncUserDb(@RequestBody SyncDbRequestBean syncRequest,
                                         @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("SyncDb Request: " + syncRequest);
        syncRequest.setRestoreService(restoreService);
        syncRequest.setHqAuth(new DjangoAuth(authToken));
        String restoreXml = syncRequest.getRestoreXml();
        CaseAPIs.restoreIfNotExists(syncRequest.getUsername(), syncRequest.getDomain(), restoreXml);
        return new SyncDbResponseBean();
    }

    @ApiOperation(value = "Get a list of the current user's sessions")
    @RequestMapping(value = Constants.URL_GET_SESSIONS, method = RequestMethod.POST)
    public GetSessionsResponse getSessions(@RequestBody GetSessionsBean getSessionRequest) throws Exception {
        log.info("Get Session Request: " + getSessionRequest);
        String username = getSessionRequest.getUsername();
        List<SerializableFormSession> sessions = sessionRepo.findUserSessions(username);
        return new GetSessionsResponse(sessions);
    }

    private void updateSession(FormSession formEntrySession, SerializableFormSession serialSession) throws IOException {
        serialSession.setFormXml(formEntrySession.getFormXml());
        serialSession.setInstanceXml(formEntrySession.getInstanceXml());
        serialSession.setSequenceId(formEntrySession.getSequenceId() + 1);
        sessionRepo.save(serialSession);
    }


    @ExceptionHandler(Exception.class)
    public String handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        exception.printStackTrace();
        JSONObject errorReturn = new JSONObject();
        errorReturn.put("message", exception);
        errorReturn.put("url", req.getRequestURL());
        errorReturn.put("status", "error");
        return errorReturn.toString();
    }



}