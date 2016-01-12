package application;

import hq.CaseAPIs;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import requests.FilterRequest;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
public class CaseController {

    @RequestMapping("/cases")
    public CaseResponse getCaseIds(@RequestParam(value="username") String username,
                                   @RequestParam(value="password") String password,
                                   @RequestParam(value="filter_expr", defaultValue="") String filterExpression){
        System.out.println("Cases!!!");
        return new CaseResponse(CaseAPIs.filterCases(username, password, filterExpression));
    }

    @RequestMapping("/filter_cases_auth")
    public CaseResponse filterCasesHQ(@RequestBody String body) throws Exception {
        FilterRequest request = new FilterRequest(body);
        String caseResponse = CaseAPIs.filterCasesAuth(request.getUsername(), request.getAuthKey(), request.getFilter());
        return new CaseResponse(caseResponse);
    }
}
