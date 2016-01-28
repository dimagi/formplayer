package application;

import beans.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
import objects.SessionList;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.json.AnswerQuestionJson;
import org.commcare.api.json.PromptToJson;
import org.commcare.api.json.WalkJson;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.SessionRepo;
import requests.NewFormRequest;
import services.RestoreService;
import services.XFormService;
import session.FormEntrySession;
import org.apache.commons.logging.Log;

import java.io.IOException;
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

    Log log = LogFactory.getLog(SessionController.class);
    ObjectMapper mapper = new ObjectMapper();

    @RequestMapping("/new_session")
    public NewSessionResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean) throws Exception {
        NewFormRequest newFormRequest = new NewFormRequest(newSessionBean, sessionRepo, xFormService, restoreService);
        return newFormRequest.getResponse();
    }


    @RequestMapping(value = "/sessions", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody List<SerializableSession> findAllSessions() {

        Map<Object, Object> mMap = sessionRepo.findAll();
        SessionList sessionList = new SessionList();

        for (Object obj : mMap.values()) {
            sessionList.add((SerializableSession) obj);
        }
        return sessionList;
    }

    @RequestMapping(value = "/get_session", method = RequestMethod.GET)
    @ResponseBody
    public SerializableSession getSession(@RequestParam(value="id") String id) {
        SerializableSession serializableSession = sessionRepo.find(id);
        return serializableSession;
    }


    @RequestMapping("/answer_question")
    public AnswerQuestionResponseBean answerQuestion(@RequestBody AnswerQuestionRequestBean answerQuestionBean) throws Exception {
        SerializableSession session = sessionRepo.find(answerQuestionBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(session);
        JSONObject resp = AnswerQuestionJson.questionAnswerToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                answerQuestionBean.getAnswer(),
                answerQuestionBean.getFormIndex());
        session.setFormXml(formEntrySession.getFormXml());
        session.setInstanceXml(formEntrySession.getInstanceXml());
        sessionRepo.save(session);
        AnswerQuestionResponseBean responseBean = mapper.readValue(resp.toString(), AnswerQuestionResponseBean.class);
        return responseBean;

    }

    @RequestMapping(value = "/current", method = RequestMethod.GET)
    @ResponseBody
    public CurrentResponseBean getCurrent(@RequestBody CurrentRequestBean currentRequestBean) throws Exception {
        SerializableSession serializableSession = sessionRepo.find(currentRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableSession);
        return new CurrentResponseBean(formEntrySession);
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    @ResponseBody
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean) throws Exception {
        SerializableSession serializableSession = sessionRepo.find(submitRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableSession);
        return new SubmitResponseBean(formEntrySession);
    }

    @RequestMapping(value = "/get_instance", method = RequestMethod.GET)
    @ResponseBody
    public GetInstanceResponseBean getInstance(@RequestBody GetInstanceRequestBean getInstanceRequestBean) throws Exception {
        SerializableSession serializableSession = sessionRepo.find(getInstanceRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableSession);
        return new GetInstanceResponseBean(formEntrySession);
    }

    @RequestMapping(value = "/evaluate_xpath", method = RequestMethod.GET)
    @ResponseBody
    public EvaluateXPathResponseBean evaluateXpath(@RequestBody EvaluateXPathRequestBean evaluateXPathRequestBean) throws Exception {
        SerializableSession serializableSession = sessionRepo.find(evaluateXPathRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableSession);
        return new EvaluateXPathResponseBean(formEntrySession, evaluateXPathRequestBean.getXpath());
    }

    @RequestMapping(value = "/new_repeat", method = RequestMethod.GET)
    @ResponseBody
    public NewRepeatResponseBean newRepeat(@RequestBody NewRepeatRequestBean newRepeatRequestBean) throws Exception {
        SerializableSession serializableSession = sessionRepo.find(newRepeatRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableSession);

        AnswerQuestionJson.descendRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                newRepeatRequestBean.getFormIndex());

        serializableSession.setFormXml(formEntrySession.getFormXml());
        serializableSession.setInstanceXml(formEntrySession.getInstanceXml());
        sessionRepo.save(serializableSession);
        JSONObject response =  AnswerQuestionJson.getCurrentJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel());
        return mapper.readValue(response.toString(), NewRepeatResponseBean.class);
    }

    @RequestMapping(value = "/delete_repeat", method = RequestMethod.GET)
    @ResponseBody
    public DeleteRepeatResponseBean delete_repeat(@RequestBody DeleteRepeatRequestBean deleteRepeatRequestBean) throws Exception {
        SerializableSession serializableSession = sessionRepo.find(deleteRepeatRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableSession);

        JSONObject resp = AnswerQuestionJson.deleteRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                deleteRepeatRequestBean.getFormIndex());

        serializableSession.setFormXml(formEntrySession.getFormXml());
        serializableSession.setInstanceXml(formEntrySession.getInstanceXml());
        sessionRepo.save(serializableSession);

        JSONObject response =  AnswerQuestionJson.getCurrentJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel());

        return mapper.readValue(response.toString(), DeleteRepeatResponseBean.class);
    }
}