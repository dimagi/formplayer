package application;

import hq.CaseAPIs;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import requests.FilterRequest;
import requests.NewFormRequest;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
@EnableAutoConfiguration
public class NewFormController {

    @RequestMapping("/new_form")
    public NewFormResponse newFormResponse(@RequestBody String body) throws Exception {
        NewFormRequest newFormRequest = new NewFormRequest(body);
        return newFormRequest.getResponse();
    }
}
