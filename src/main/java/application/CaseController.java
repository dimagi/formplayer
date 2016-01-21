package application;

import com.fasterxml.jackson.databind.ObjectMapper;
import hq.CaseAPIs;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import requests.FilterRequest;

import java.util.logging.Filter;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
@EnableAutoConfiguration
public class CaseController {

    @RequestMapping("/filter_cases")
    public CaseResponse filterCasesHQ(@RequestBody String body) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        FilterRequest filterRequest = mapper.readValue(body, FilterRequest.class);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        return new CaseResponse(caseResponse);
    }
}
