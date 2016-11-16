package application;

import auth.DjangoAuth;
import beans.*;
import hq.CaseAPIs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import util.Constants;

/**
 * Controller class (API endpoint) containing all all logic that isn't associated with
 * a particular form session or menu navigation. Includes:
 *      Get Cases
 *      Filter Cases
 *      Sync User DB
 *      Get Sessions (Incomplete Forms)
 */
@Api(value = "Util Controller", description = "Operations that aren't associated with form or menu navigation")
@RestController
@EnableAutoConfiguration
public class UtilController extends AbstractBaseController {

    @ApiOperation(value = "Filter the user's casedb given a predicate expression")
    @RequestMapping(value = Constants.URL_FILTER_CASES, method = RequestMethod.GET)
    public CaseFilterResponseBean filterCasesHQ(@RequestBody CaseFilterRequestBean filterRequest,
                                                @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        restoreFactory.configure(filterRequest, new DjangoAuth(authToken));
        String caseResponse = CaseAPIs.filterCases(restoreFactory, filterRequest.getFilterExpression());
        return new CaseFilterResponseBean(caseResponse);
    }

    @ApiOperation(value = "Fitler the user's casedb given a predicate expression returning all case data")
    @RequestMapping(value = Constants.URL_FILTER_CASES_FULL, method = RequestMethod.GET)
    public CaseFilterFullResponseBean filterCasesFull(@RequestBody CaseFilterRequestBean filterRequest,
                                                      @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken)throws Exception {
        restoreFactory.configure(filterRequest, new DjangoAuth(authToken));
        CaseBean[] caseResponse = CaseAPIs.filterCasesFull(restoreFactory, filterRequest.getFilterExpression());
        return new CaseFilterFullResponseBean(caseResponse);
    }

    @ApiOperation(value = "Sync the user's database with the server")
    @RequestMapping(value = Constants.URL_SYNC_DB, method = RequestMethod.POST)
    public SyncDbResponseBean syncUserDb(@RequestBody SyncDbRequestBean syncRequest,
                                         @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        restoreFactory.configure(syncRequest, new DjangoAuth(authToken));
        CaseAPIs.forceRestore(restoreFactory);
        return new SyncDbResponseBean();
    }

    @ApiOperation(value = "Wipe the applications databases")
    @RequestMapping(value = Constants.URL_DELETE_APPLICATION_DBS, method = RequestMethod.POST)
    public NotificationMessageBean deleteApplicationDbs(
            @RequestBody DeleteApplicationDbsRequestBean deleteRequest) {

        String message = "Successfully cleared application database for " + deleteRequest.getAppId();
        boolean success = deleteRequest.clear();
        if (success) {
            message = "Failed to clear application database for " + deleteRequest.getAppId();
        }
        return new NotificationMessageBean(message, !success);
    }

    @ApiOperation(value = "Gets the status of the Formplayer service")
    @RequestMapping(value = Constants.URL_SERVER_UP, method = RequestMethod.GET)
    public ServerUpBean serverUp() throws Exception {
        return new ServerUpBean();
    }
}
