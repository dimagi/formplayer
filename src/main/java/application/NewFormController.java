package application;

import hq.CaseAPIs;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import requests.FilterRequest;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
public class NewFormController {

    @RequestMapping("/new_form")
    public String newForm(@RequestParam("session-id") String sessionId) throws Exception {
        System.out.println("MObject: " + sessionId);
        return "derp";
    }
}
