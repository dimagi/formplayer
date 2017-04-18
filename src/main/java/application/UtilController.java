package application;

import annotations.NoLogging;
import annotations.UserLock;
import annotations.UserRestore;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.*;
import hq.CaseAPIs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.schema.JSONReporter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import sandbox.UserSqlSandbox;
import util.Constants;

import java.io.StringReader;

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

    @ApiOperation(value = "Sync the user's database with the server")
    @RequestMapping(value = Constants.URL_SYNC_DB, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    public SyncDbResponseBean syncUserDb(@RequestBody SyncDbRequestBean syncRequest,
                                         @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        if (syncRequest.isPreserveCache()) {
            CaseAPIs.restoreIfNotExists(restoreFactory, false);
        } else {
            CaseAPIs.forceRestore(restoreFactory);
        }
        return new SyncDbResponseBean();
    }

    @ApiOperation(value = "Wipe the applications databases")
    @RequestMapping(value = Constants.URL_DELETE_APPLICATION_DBS, method = RequestMethod.POST)
    @UserLock
    public NotificationMessageBean deleteApplicationDbs(
            @RequestBody DeleteApplicationDbsRequestBean deleteRequest) {

        String message = "Successfully cleared application database for " + deleteRequest.getAppId();
        boolean success = deleteRequest.clear();
        if (!success) {
            message = "Failed to clear application database for " + deleteRequest.getAppId();
        }
        return new NotificationMessageBean(message, !success);
    }

    @ApiOperation(value = "Gets the status of the Formplayer service")
    @RequestMapping(value = Constants.URL_SERVER_UP, method = RequestMethod.GET)
    public ServerUpBean serverUp() throws Exception {
        return new ServerUpBean();
    }

    @ApiOperation(value = "Validates an XForm")
    @NoLogging
    @RequestMapping(
        value = Constants.URL_VALIDATE_FORM,
        method = RequestMethod.POST,
        produces = { MediaType.APPLICATION_JSON_VALUE },
        consumes = { MediaType.APPLICATION_XML_VALUE}
    )
    public String validateForm(@RequestBody String formXML) throws Exception {
        JSONReporter reporter = new JSONReporter();
        try {
            XFormParser parser = new XFormParser(new StringReader(formXML));
            parser.attachReporter(reporter);
            parser.parse();
            reporter.setPassed();
        } catch (XFormParseException xfpe) {
            reporter.setFailed(xfpe);
        } catch (Exception e) {
            reporter.setFailed(e);
        }

        return reporter.generateJSONReport();
    }
}
