package application;

import beans.AnswerQuestionBean;
import beans.AnswerQuestionResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import hq.CaseAPIs;
import objects.SerializableSession;
import objects.SessionList;
import org.apache.commons.io.IOUtils;
import org.commcare.api.json.AnswerQuestionJson;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParser;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.SessionRepo;
import requests.FilterRequest;
import requests.NewFormRequest;
import services.XFormService;

import java.util.ArrayList;
import java.util.Arrays;
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

    @RequestMapping("/new_session")
    public NewFormResponse newFormResponse(@RequestBody String body) throws Exception {
        NewFormRequest newFormRequest = new NewFormRequest(body, sessionRepo, xFormService);
        return newFormRequest.getResponse();
    }


    @RequestMapping(value = "/sessions", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody List<SerializableSession> findAllSessions() {

        Map<Object, Object> mMap = sessionRepo.findAll();
        SessionList sessionList = new SessionList();

        for(Object obj: mMap.values()){
            sessionList.add((SerializableSession)obj);
        }
        System.out.println("Return Session List " + sessionList);
        return sessionList;
    }

    @RequestMapping(value = "/get_session", method = RequestMethod.GET)
    @ResponseBody
    public SerializableSession getSession(@RequestParam(value="id") String id) {
        System.out.println("Getting session: " + id);
        SerializableSession serializableSession = sessionRepo.find(id);
        return serializableSession;
    }


    @RequestMapping("/answer_question")
    public AnswerQuestionResponseBean answerQuestion(@RequestBody String body) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AnswerQuestionBean answerQuestionBean = mapper.readValue(body, AnswerQuestionBean.class);
        System.out.println("Answer Question Bean: " + answerQuestionBean.getSessionId());
        SerializableSession session = sessionRepo.find(answerQuestionBean.getSessionId());
        FormInstance formInstance = XFormParser.restoreDataModel(IOUtils.toInputStream(session.getInstanceXml()), null);
        FormDef formDef = new FormDef();
        formDef.setInstance(formInstance);
        FormEntryModel fem = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_LINEAR);
        FormEntryController fec = new FormEntryController(fem);
        JSONObject resp = AnswerQuestionJson.questionAnswerToJson(fec, fem,
                answerQuestionBean.getAnswer(), answerQuestionBean.getFormIndex());
        AnswerQuestionResponseBean responseBean = mapper.readValue(resp.toString(), AnswerQuestionResponseBean.class);
        return responseBean;

    }
}
