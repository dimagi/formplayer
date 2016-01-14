package application;

import hq.CaseAPIs;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import requests.FilterRequest;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
@EnableAutoConfiguration
public class CaseController {

    @RequestMapping("/cases")
    public CaseResponse getCaseIds(@RequestParam(value="username") String username,
                                   @RequestParam(value="password") String password,
                                   @RequestParam(value="domain", defaultValue="test") String domain,
                                   @RequestParam(value="host", defaultValue="localhost:8000") String host,
                                   @RequestParam(value="filter_expr", defaultValue="") String filterExpression) throws Exception {
        FilterRequest filterRequest = new FilterRequest(username, password, domain, host, filterExpression);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        return new CaseResponse(caseResponse);
    }

    @RequestMapping("/filter_cases")
    public CaseResponse filterCasesHQ(@RequestBody String body) throws Exception {
        FilterRequest filterRequest = new FilterRequest(body);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        return new CaseResponse(caseResponse);
    }
}
