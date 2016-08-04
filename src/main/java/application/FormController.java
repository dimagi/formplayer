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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repo.SerializableMenuSession;
import requests.NewFormRequest;
import services.SubmitService;
import services.XFormService;
import session.FormSession;
import session.MenuSession;
import util.Constants;

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

    private final Log log = LogFactory.getLog(FormController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @ApiOperation(value = "Start a new form entry session")
    @RequestMapping(value = Constants.URL_NEW_SESSION , method = RequestMethod.POST)
    public NewFormSessionResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean,
                                                  @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("New form requests with bean: " + newSessionBean + " sessionId :" + authToken);
        NewFormRequest newFormRequest = new NewFormRequest(newSessionBean, formSessionRepo, xFormService, restoreService, authToken);
        NewFormSessionResponse newSessionResponse = newFormRequest.getResponse();
        log.info("Return new session response: " + newSessionResponse);
        return newSessionResponse;
    }

    @ApiOperation(value = "Open an incomplete form session")
    @RequestMapping(value = Constants.URL_INCOMPLETE_SESSION , method = RequestMethod.POST)
    public NewFormSessionResponse openIncompleteForm(@RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
                                                  @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Incomplete session request with bean: " + incompleteSessionRequestBean + " sessionId :" + authToken);
        SerializableFormSession session = formSessionRepo.findOne(incompleteSessionRequestBean.getSessionId());
        NewFormRequest newFormRequest = new NewFormRequest(session, restoreService, authToken);
        NewFormSessionResponse response = newFormRequest.getResponse();
        log.info("Return incomplete session response: " + response);
        return response;
    }

    @ApiOperation(value = "Answer the question at the given index")
    @RequestMapping(value = Constants.URL_ANSWER_QUESTION, method = RequestMethod.POST)
    public FormEntryResponseBean answerQuestion(@RequestBody AnswerQuestionRequestBean answerQuestionBean) throws Exception {
        log.info("Answer question with bean: " + answerQuestionBean);
        SerializableFormSession session = formSessionRepo.findOne(answerQuestionBean.getSessionId());
        FormSession formEntrySession = new FormSession(session);
        JSONObject resp = JsonActionUtils.questionAnswerToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                answerQuestionBean.getAnswer() != null? answerQuestionBean.getAnswer().toString() : null,
                answerQuestionBean.getFormIndex());
        updateSession(formEntrySession, session);

        FormEntryResponseBean responseBean = mapper.readValue(resp.toString(), FormEntryResponseBean.class);
        responseBean.setSequenceId(formEntrySession.getSequenceId());
        log.info("Answer response: " + responseBean);
        return responseBean;
    }

    @ApiOperation(value = "Get the current question (deprecated)")
    @RequestMapping(value = Constants.URL_CURRENT, method = RequestMethod.GET)
    @ResponseBody
    public FormEntryResponseBean getCurrent(@RequestBody CurrentRequestBean currentRequestBean) throws Exception {
        log.info("Current request: " + currentRequestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOne(currentRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        JSONObject resp = JsonActionUtils.getCurrentJson(formEntrySession.getFormEntryController(), formEntrySession.getFormEntryModel());
        FormEntryResponseBean responseBean = mapper.readValue(resp.toString(), FormEntryResponseBean.class);
        log.info("Current response: " + responseBean);
        return responseBean;
    }

    @ApiOperation(value = "Submit the current form")
    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    public Object submitForm(@RequestBody SubmitRequestBean submitRequestBean,
                                             @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Submit form with bean: " + submitRequestBean);

        SerializableFormSession serializableFormSession = formSessionRepo.findOne(submitRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);

        log.info("Submitting form entry session has menuId: " + formEntrySession.getMenuSessionId());

        SubmitResponseBean submitResponseBean = validateSubmitAnswers(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                submitRequestBean.getAnswers());

        if(!submitResponseBean.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE)
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
                log.info("Submit response bean: " + submitResponseBean);
                return submitResponseBean;
                //TODO: need new way to communicate errors with submitting (vs. validation)
            }
            if(formEntrySession.getMenuSessionId() != null &&
                    !("").equals(formEntrySession.getMenuSessionId().trim())) {
                Object nav = doEndOfFormNav(menuSessionRepo.findOne(formEntrySession.getMenuSessionId()), new DjangoAuth(authToken));
                if(nav != null){
                    submitResponseBean.setNextScreen(nav);
                }
            }
            formSessionRepo.delete(submitRequestBean.getSessionId());

        }
        log.info("Submit response bean: " + submitResponseBean);
        return submitResponseBean;
    }

    private Object doEndOfFormNav(SerializableMenuSession serializedSession, HqAuth auth) throws Exception {
        log.info("End of form navigation with serialized menu session: " + serializedSession);
        MenuSession menuSession = new MenuSession(serializedSession, installService, restoreService, auth);
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
    public GetInstanceResponseBean getInstance(@RequestBody GetInstanceRequestBean getInstanceRequestBean) throws Exception {
        log.info("Get instance request: " + getInstanceRequestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOne(getInstanceRequestBean.getSessionId());
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
        SerializableFormSession serializableFormSession = formSessionRepo.findOne(evaluateXPathRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        EvaluateXPathResponseBean evaluateXPathResponseBean =
                new EvaluateXPathResponseBean(formEntrySession, evaluateXPathRequestBean.getXpath());
        log.info("Evaluate XPath Response: " + evaluateXPathResponseBean);
        return evaluateXPathResponseBean;
    }

    @ApiOperation(value = "Expand the repeat at the given index")
    @RequestMapping(value = Constants.URL_NEW_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    public FormEntryResponseBean newRepeat(@RequestBody RepeatRequestBean newRepeatRequestBean) throws Exception {
        log.info("New repeat: " + newRepeatRequestBean);
        SerializableFormSession serializableFormSession = formSessionRepo.findOne(newRepeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        JSONObject response = JsonActionUtils.descendRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                newRepeatRequestBean.getRepeatIndex());
        updateSession(formEntrySession, serializableFormSession);
        FormEntryResponseBean repeatResponseBean = mapper.readValue(response.toString(), FormEntryResponseBean.class);
        log.info("New response: " + repeatResponseBean);
        return repeatResponseBean;
    }

    @ApiOperation(value = "Delete the repeat at the given index")
    @RequestMapping(value = Constants.URL_DELETE_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    public FormEntryResponseBean deleteRepeat(@RequestBody RepeatRequestBean deleteRepeatRequestBean) throws Exception {
        SerializableFormSession serializableFormSession = formSessionRepo.findOne(deleteRepeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        JSONObject resp = JsonActionUtils.deleteRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                deleteRepeatRequestBean.getRepeatIndex(), deleteRepeatRequestBean.getFormIndex());
        updateSession(formEntrySession, serializableFormSession);
        return mapper.readValue(resp.toString(), FormEntryResponseBean.class);
    }

    private void updateSession(FormSession formEntrySession, SerializableFormSession serialSession) throws IOException {
        formEntrySession.setSequenceId(formEntrySession.getSequenceId() + 1);
        serialSession.setFormXml(formEntrySession.getFormXml());
        serialSession.setInstanceXml(formEntrySession.getInstanceXml());
        serialSession.setSequenceId(formEntrySession.getSequenceId());
        formSessionRepo.save(serialSession);
    }
}