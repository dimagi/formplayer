package application;

import com.fasterxml.jackson.databind.ObjectMapper;
import hq.CaseAPIs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import requests.FilterRequest;
import services.RestoreService;
import services.XFormService;

import java.util.logging.Filter;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
@EnableAutoConfiguration
public class CaseController {

    @Autowired
    private RestoreService restoreService;

    @RequestMapping("/filter_cases")
    public CaseResponse filterCasesHQ(@RequestBody String body) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("Filter body: " + body);
        FilterRequest filterRequest = mapper.readValue(body, FilterRequest.class);
        filterRequest.setRestoreService(restoreService);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        System.out.println("case reponse: " + caseResponse);
        return new CaseResponse(caseResponse);
    }
}
