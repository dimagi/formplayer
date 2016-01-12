package application;

import hq.CaseAPIs;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
public class CaseController {

    @RequestMapping("/cases")
    public String getCaseIds(@RequestParam(value="username") String username,
                                  @RequestParam(value="password") String password,
                                  @RequestParam(value="filter_expr", defaultValue="") String filterExpression){
        return CaseAPIs.filterCases(username, password, filterExpression);
    }
}
