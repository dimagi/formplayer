package application;

import beans.CaseFilterRequestBean;
import beans.CaseFilterResponseBean;
import beans.SyncDbRequestBean;
import beans.SyncDbResponseBean;
import hq.CaseAPIs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    public CaseFilterResponseBean filterCasesHQ(@RequestBody CaseFilterRequestBean filterRequest) throws Exception {
        filterRequest.setRestoreService(restoreService);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        return new CaseFilterResponseBean(caseResponse);
    }

    @RequestMapping("/sync_db")
    public SyncDbResponseBean syncUserDb(@RequestBody SyncDbRequestBean syncRequest) throws Exception {
        syncRequest.setRestoreService(restoreService);
        String restoreXml = syncRequest.getRestoreXml();
        CaseAPIs.restoreIfNotExists(syncRequest.getUsername(), restoreXml);
        return new SyncDbResponseBean();
    }
}
