package application;

import annotations.NoLogging;
import annotations.UserLock;
import annotations.UserRestore;
import beans.*;
import hq.CaseAPIs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.schema.JSONReporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import services.CategoryTimingHelper;
import sqlitedb.UserDB;
import util.Constants;
import util.FormplayerHttpRequest;
import util.Timing;

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

    private final Log log = LogFactory.getLog(UtilController.class);

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @ApiOperation(value = "Sync the user's database with the server")
    @RequestMapping(value = Constants.URL_SYNC_DB, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    public SyncDbResponseBean syncUserDb(@RequestBody SyncDbRequestBean syncRequest,
                                         @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        restoreFactory.performTimedSync();
        return new SyncDbResponseBean();
    }

    @ApiOperation(value = "Wipe the applications databases")
    @RequestMapping(value = {Constants.URL_DELETE_APPLICATION_DBS, Constants.URL_UPDATE}, method = RequestMethod.POST)
    @UserLock
    public NotificationMessage deleteApplicationDbs(
            @RequestBody DeleteApplicationDbsRequestBean deleteRequest,
            FormplayerHttpRequest request) {

        String message = "Successfully cleared application database for " + deleteRequest.getAppId();
        boolean success = true;
        try {
            deleteRequest.clear();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }

        if (!success) {
            message = "Failed to clear application database for " + deleteRequest.getAppId();
        }
        NotificationMessage notificationMessage = new NotificationMessage(message, !success, NotificationMessage.Tag.wipedb);
        logNotification(notificationMessage, request);
        return notificationMessage;
    }

    @ApiOperation(value = "Clear the user's data")
    @RequestMapping(value = Constants.URL_CLEAR_USER_DATA, method = RequestMethod.POST)
    @UserLock
    public NotificationMessage clearUserData(
            @RequestBody AuthenticatedRequestBean requestBean,
            FormplayerHttpRequest request) {

        String message = "Successfully cleared the user data for  " + requestBean.getUsername();
        new UserDB(
                requestBean.getDomain(),
                requestBean.getUsername(),
                requestBean.getRestoreAs()
        ).deleteDatabaseFolder();
        NotificationMessage notificationMessage = new NotificationMessage(message, true, NotificationMessage.Tag.clear_data);
        logNotification(notificationMessage, request);
        return notificationMessage;
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
            log.error("Validate Form threw exception", xfpe);
            reporter.setFailed(xfpe);
        } catch (Exception e) {
            log.error("Validate Form threw exception", e);
            reporter.setFailed(e);
        }

        return reporter.generateJSONReport();
    }
}
