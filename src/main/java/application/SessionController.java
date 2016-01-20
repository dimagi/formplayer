package application;

import hq.CaseAPIs;
import objects.SerializableSession;
import objects.SessionList;
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
}
