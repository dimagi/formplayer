package application;

import beans.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
import objects.SessionList;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.json.AnswerQuestionJson;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.SessionRepo;
import requests.NewFormRequest;
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

    Log log = LogFactory.getLog(SessionController.class);

    @RequestMapping("/new_session")
    public NewSessionResponse newFormResponse(@RequestBody String body){
        try {
            ObjectMapper mapper = new ObjectMapper();
            NewSessionBean newSessionBean = mapper.readValue(body, NewSessionBean.class);
            NewFormRequest newFormRequest = new NewFormRequest(newSessionBean, sessionRepo, xFormService);
            return newFormRequest.getResponse();
        } catch (JsonParseException e) {
            log.error(e);
        } catch (JsonMappingException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
        return null;
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
    public AnswerQuestionResponseBean answerQuestion(@RequestBody String body){
        try {
            ObjectMapper mapper = new ObjectMapper();
            AnswerQuestionRequestBean answerQuestionBean = mapper.readValue(body, AnswerQuestionRequestBean.class);
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
        } catch (JsonParseException e) {
            log.error(e);
        } catch (JsonMappingException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
        return null;

    }

    @RequestMapping(value = "/current", method = RequestMethod.GET)
    @ResponseBody
    public CurrentResponseBean getCurrent(@RequestBody String body) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            CurrentRequestBean currentRequestBean = null;
            currentRequestBean = mapper.readValue(body, CurrentRequestBean.class);
            SerializableSession serializableSession = sessionRepo.find(currentRequestBean.getSessionId());
            FormEntrySession formEntrySession = new FormEntrySession(serializableSession);
            return new CurrentResponseBean(formEntrySession);
        } catch (IOException e) {
            log.error(e);
        }
        //error handling?
        return null;
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    @ResponseBody
    public SubmitResponseBean submitForm(@RequestBody String body) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SubmitRequestBean submitRequestBean = mapper.readValue(body, SubmitRequestBean.class);
        SerializableSession serializableSession = sessionRepo.find(submitRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableSession);
        return new SubmitResponseBean(formEntrySession);
    }

    @RequestMapping(value = "/get_instance", method = RequestMethod.GET)
    @ResponseBody
    public GetInstanceResponseBean getInstance(@RequestBody String body) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        GetInstanceRequestBean getInstanceRequestBean = mapper.readValue(body, GetInstanceRequestBean.class);
        SerializableSession serializableSession = sessionRepo.find(getInstanceRequestBean.getSessionId());
        FormEntrySession formEntrySession = new FormEntrySession(serializableSession);
        return new GetInstanceResponseBean(formEntrySession);
    }

    @RequestMapping(value = "/evaluate_xpath", method = RequestMethod.GET)
    @ResponseBody
    public EvaluateXPathResponseBean evaluateXpath(@RequestBody String body) {
        try {
            System.out.println("Evaluation called");
            ObjectMapper mapper = new ObjectMapper();
            EvaluateXPathRequestBean evaluateXPathRequestBean = mapper.readValue(body, EvaluateXPathRequestBean.class);
            SerializableSession serializableSession = sessionRepo.find(evaluateXPathRequestBean.getSessionId());
            FormEntrySession formEntrySession = new FormEntrySession(serializableSession);
            return new EvaluateXPathResponseBean(formEntrySession, evaluateXPathRequestBean.getXpath());
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
