package application;

import beans.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import hq.CaseAPIs;
import objects.SerializableFormSession;
import objects.SessionList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.json.AnswerQuestionJson;
import org.commcare.modern.process.FormRecordProcessorHelper;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.util.cli.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import repo.MenuRepo;
import repo.SessionRepo;
import requests.InstallRequest;
import requests.NewFormRequest;
import services.RestoreService;
import services.XFormService;
import session.FormEntrySession;
import session.MenuSession;
import util.Constants;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
@EnableAutoConfiguration
public class SessionController {

    @Autowired
    private SessionRepo sessionRepo;

    @Autowired
    private XFormService xFormService;

    @Autowired
    private RestoreService restoreService;

    @Autowired
    private MenuRepo menuRepo;

    Log log = LogFactory.getLog(SessionController.class);
    ObjectMapper mapper = new ObjectMapper();

    @RequestMapping(Constants.URL_NEW_SESSION)
    public NewSessionResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean) throws Exception {
        log.info("New form requests with bean: " + newSessionBean);
        NewFormRequest newFormRequest = new NewFormRequest(newSessionBean, sessionRepo, xFormService, restoreService);
        NewSessionResponse newSessionResponse = newFormRequest.getResponse();
        log.info("Return new session response: " + newSessionResponse);
        return newSessionResponse;
    }

    @RequestMapping(value = Constants.URL_LIST_SESSIONS, method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody List<SerializableFormSession> findAllSessions() {
        Map<Object, Object> mMap = sessionRepo.findAll();
        SessionList sessionList = new SessionList();

        for (Object obj : mMap.values()) {
            sessionList.add((SerializableFormSession) obj);
        }
        return sessionList;
    }

    @RequestMapping(value = Constants.URL_GET_SESSION, method = RequestMethod.GET)
    @ResponseBody
    public SerializableFormSession getSession(@RequestParam(value="id") String id) {
        SerializableFormSession serializableFormSession = sessionRepo.find(id);
        return serializableFormSession;
    }


    @RequestMapping(Constants.URL_ANSWER_QUESTION)
    public AnswerQuestionResponseBean answerQuestion(@RequestBody AnswerQuestionRequestBean answerQuestionBean) throws Exception {
        log.info("Answer question with bean: " + answerQuestionBean);
        SerializableFormSession session = sessionRepo.find(answerQuestionBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(session);

        JSONObject resp = AnswerQuestionJson.questionAnswerToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                answerQuestionBean.getAnswer() != null? answerQuestionBean.getAnswer().toString() : null,
                answerQuestionBean.getFormIndex());

        session.setFormXml(formEntrySession.getFormXml());
        session.setInstanceXml(formEntrySession.getInstanceXml());
        session.setSequenceId(formEntrySession.getSequenceId() + 1);
        sessionRepo.save(session);
        AnswerQuestionResponseBean responseBean = mapper.readValue(resp.toString(), AnswerQuestionResponseBean.class);
        responseBean.setSequenceId(formEntrySession.getSequenceId() + 1);
        log.info("Answer response: " + responseBean);
        return responseBean;
    }

    @RequestMapping(value = Constants.URL_CURRENT, method = RequestMethod.GET)
    @ResponseBody
    public CurrentResponseBean getCurrent(@RequestBody CurrentRequestBean currentRequestBean) throws Exception {
        log.info("Current request: " + currentRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(currentRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableFormSession);
        CurrentResponseBean currentResponseBean = new CurrentResponseBean(formEntrySession);
        log.info("Current response: " + currentResponseBean);
        return currentResponseBean;
    }

    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean) throws Exception {
        log.info("Submit form with bean: " + submitRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(submitRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableFormSession);
        FormRecordProcessorHelper.processXML(formEntrySession.getSandbox(), formEntrySession.submitGetXml());
        SubmitResponseBean submitResponseBean = new SubmitResponseBean(formEntrySession);
        log.info("Submit response bean: " + submitResponseBean);
        return submitResponseBean;
    }

    @RequestMapping(value = Constants.URL_GET_INSTANCE)
    @ResponseBody
    public GetInstanceResponseBean getInstance(@RequestBody GetInstanceRequestBean getInstanceRequestBean) throws Exception {
        log.info("Get instance request: " + getInstanceRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(getInstanceRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableFormSession);
        GetInstanceResponseBean getInstanceResponseBean = new GetInstanceResponseBean(formEntrySession);
        log.info("Get instance response: " + getInstanceResponseBean);
        return getInstanceResponseBean;
    }

    @RequestMapping(value = Constants.URL_EVALUATE_XPATH, method = RequestMethod.GET)
    @ResponseBody
    public EvaluateXPathResponseBean evaluateXpath(@RequestBody EvaluateXPathRequestBean evaluateXPathRequestBean) throws Exception {
        SerializableFormSession serializableFormSession = sessionRepo.find(evaluateXPathRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableFormSession);
        return new EvaluateXPathResponseBean(formEntrySession, evaluateXPathRequestBean.getXpath());
    }

    @RequestMapping(value = Constants.URL_NEW_REPEAT, method = RequestMethod.GET)
    @ResponseBody
    public RepeatResponseBean newRepeat(@RequestBody RepeatRequestBean newRepeatRequestBean) throws Exception {
        log.info("New repeat: " + newRepeatRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(newRepeatRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableFormSession);

        AnswerQuestionJson.descendRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                newRepeatRequestBean.getFormIndex());

        serializableFormSession.setFormXml(formEntrySession.getFormXml());
        serializableFormSession.setInstanceXml(formEntrySession.getInstanceXml());
        sessionRepo.save(serializableFormSession);
        JSONObject response = AnswerQuestionJson.getCurrentJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel());
        RepeatResponseBean repeatResponseBean = mapper.readValue(response.toString(), RepeatResponseBean.class);
        log.info("New response: " + repeatResponseBean);
        return repeatResponseBean;
    }

    @RequestMapping(value = Constants.URL_DELETE_REPEAT, method = RequestMethod.GET)
    @ResponseBody
    public RepeatResponseBean deleteRepeat(@RequestBody RepeatRequestBean repeatRequestBean) throws Exception {
        SerializableFormSession serializableFormSession = sessionRepo.find(repeatRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableFormSession);

        JSONObject resp = AnswerQuestionJson.deleteRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                repeatRequestBean.getFormIndex());

        serializableFormSession.setFormXml(formEntrySession.getFormXml());
        serializableFormSession.setInstanceXml(formEntrySession.getInstanceXml());
        sessionRepo.save(serializableFormSession);

        JSONObject response =  AnswerQuestionJson.getCurrentJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel());

        return mapper.readValue(response.toString(), RepeatResponseBean.class);
    }

    @RequestMapping(Constants.URL_FILTER_CASES)
    public CaseFilterResponseBean filterCasesHQ(@RequestBody CaseFilterRequestBean filterRequest) throws Exception {
        filterRequest.setRestoreService(restoreService);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        return new CaseFilterResponseBean(caseResponse);
    }

    @RequestMapping(Constants.URL_FILTER_CASES_FULL)
    public CaseFilterFullResponseBean filterCasesFull(@RequestBody CaseFilterRequestBean filterRequest) throws Exception {
        filterRequest.setRestoreService(restoreService);
        CaseBean[] caseResponse = CaseAPIs.filterCasesFull(filterRequest);
        return new CaseFilterFullResponseBean(caseResponse);
    }

    @RequestMapping(Constants.URL_SYNC_DB)
    public SyncDbResponseBean syncUserDb(@RequestBody SyncDbRequestBean syncRequest) throws Exception {
        syncRequest.setRestoreService(restoreService);
        String restoreXml = syncRequest.getRestoreXml();
        CaseAPIs.restoreIfNotExists(syncRequest.getUsername(), restoreXml);
        return new SyncDbResponseBean();
    }

    @RequestMapping(Constants.URL_INSTALL)
    public MenuResponseBean performInstall(@RequestBody InstallRequestBean installRequestBean) throws Exception {
        InstallRequest installRequest = new InstallRequest(installRequestBean, restoreService, menuRepo);
        return installRequest.getResponse();
    }

    @RequestMapping(Constants.URL_MENU_SELECT)
    public SessionBean selectMenu(@RequestBody MenuSelectBean menuSelectBean) throws Exception {
        MenuSession menuSession = new MenuSession(menuRepo.find(menuSelectBean.getSessionId()), restoreService);
        boolean redrawing = menuSession.handleInput(menuSelectBean.getSelection());
        menuRepo.save(menuSession.serialize());
        Screen nextScreen;

        if(!redrawing){
            nextScreen = menuSession.getNextScreen();
        } else{
            nextScreen = menuSession.getCurrentScreen();
        }

        if(nextScreen instanceof MenuScreen){
            MenuScreen menuScreen = (MenuScreen) nextScreen;
            MenuDisplayable[] options = menuScreen.getChoices();
            HashMap<Integer, String> optionsStrings = new HashMap<Integer, String>();
            for(int i=0; i <options.length; i++){
                optionsStrings.put(i, options[i].getDisplayText());
            }
            MenuResponseBean menuResponseBean = new MenuResponseBean();
            menuResponseBean.setMenuType(Constants.MENU_MODULE);
            menuResponseBean.setOptions(optionsStrings);
            menuResponseBean.setSessionId(menuSession.getSessionId());
            return menuResponseBean;
        } else if (nextScreen instanceof EntityScreen){
            EntityScreen entityScreen = (EntityScreen) nextScreen;

            if (entityScreen.getCurrentScreen() instanceof EntityListSubscreen) {
                EntityListSubscreen entityListSubscreen = (EntityListSubscreen) entityScreen.getCurrentScreen();
                String[] rows = entityListSubscreen.getRows();
                HashMap<Integer, String> optionsStrings = new HashMap<Integer, String>();
                for(int i=0; i <rows.length; i++){
                    optionsStrings.put(i, rows[i]);
                }
                MenuResponseBean menuResponseBean = new MenuResponseBean();
                menuResponseBean.setMenuType(Constants.MENU_ENTITY);
                menuResponseBean.setOptions(optionsStrings);
                menuResponseBean.setSessionId(menuSession.getSessionId());
                return menuResponseBean;
            } else if(entityScreen.getCurrentScreen() instanceof EntityDetailSubscreen){

                EntityDetailSubscreen entityDetailSubscreen = (EntityDetailSubscreen) entityScreen.getCurrentScreen();
                String[] rows = entityDetailSubscreen.getOptions();
                HashMap<Integer, String> optionsStrings = new HashMap<Integer, String>();
                for(int i=0; i <rows.length; i++){
                    optionsStrings.put(i, rows[i]);
                }
                MenuResponseBean menuResponseBean = new MenuResponseBean();
                menuResponseBean.setMenuType(Constants.MENU_ENTITY);
                menuResponseBean.setOptions(optionsStrings);
                menuResponseBean.setSessionId(menuSession.getSessionId());
                return menuResponseBean;
            }

        }else if (nextScreen == null){
            NewSessionResponse response = menuSession.startFormEntry(sessionRepo);
            String stringResponse = new ObjectMapper().writeValueAsString(response);
            return response;
        }
        return null;
    }

    @ExceptionHandler(Exception.class)
    public String handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        exception.printStackTrace();

        JSONObject errorReturn = new JSONObject();
        errorReturn.put("exception", exception);
        errorReturn.put("url", req.getRequestURL());
        errorReturn.put("status", "error");
        return errorReturn.toString();
    }

}