package application;

import beans.CaseFilterResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import hq.CaseAPIs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import beans.CaseFilterRequestBean;
import services.RestoreService;

/**
 * Created by willpride on 1/12/16.
 */
@RestController
@EnableAutoConfiguration
public class CaseController {

    @Autowired
    private RestoreService restoreService;

    @RequestMapping("/filter_cases")
    public CaseFilterResponseBean filterCasesHQ(@RequestBody String body) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("Filter body: " + body);
        CaseFilterRequestBean filterRequest = mapper.readValue(body, CaseFilterRequestBean.class);
        filterRequest.setRestoreService(restoreService);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        System.out.println("case reponse: " + caseResponse);
        return new CaseFilterResponseBean(caseResponse);
    }
}
