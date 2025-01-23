package org.commcare.formplayer.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.annotations.AppInstall;
import org.commcare.formplayer.annotations.NoLogging;
import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.aspects.LockAspect;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.beans.DeleteApplicationDbsRequestBean;
import org.commcare.formplayer.beans.LockReportBean;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.ServerUpBean;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.SyncDbRequestBean;
import org.commcare.formplayer.beans.SyncDbResponseBean;
import org.commcare.formplayer.services.CaseSearchHelper;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.services.FormplayerLockRegistry;
import org.commcare.formplayer.services.ResponseMetaDataTracker;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.sqlitedb.CaseSearchDB;
import org.commcare.formplayer.sqlitedb.UserDB;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.NotificationLogger;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.schema.JSONReporter;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringReader;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller class (API endpoint) containing all all logic that isn't associated with
 * a particular form session or menu navigation. Includes:
 *      Get Cases
 *      Filter Cases
 *      Sync User DB
 *      Get Sessions (Incomplete Forms)
 */
@RestController
@EnableAutoConfiguration
public class UtilController {

    @Autowired
    protected RestoreFactory restoreFactory;

    @Autowired
    private ResponseMetaDataTracker responseMetaDataTracker;

    @Autowired
    protected FormSessionService formSessionService;

    @Autowired
    FormplayerLockRegistry userLockRegistry;

    @Autowired
    NotificationLogger notificationLogger;

    private final Log log = LogFactory.getLog(UtilController.class);

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @RequestMapping(value = Constants.URL_SYNC_DB, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    public SyncDbResponseBean syncUserDb(@RequestBody SyncDbRequestBean syncRequest,
                                         @CookieValue(value = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) throws Exception {
        restoreFactory.performTimedSync();
        return new SyncDbResponseBean();
    }

    @RequestMapping(value = Constants.URL_INTERVAL_SYNC_DB, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @AppInstall
    public SyncDbResponseBean scheduleSync(@RequestBody SessionNavigationBean sessionNavigationBean,
            @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken,
            HttpServletRequest request) throws Exception {
        SyncDbResponseBean response = new SyncDbResponseBean();
        if (restoreFactory.isRestoreXmlExpired()) {
            restoreFactory.performTimedSync();
        }
        response.setAttemptRestore(responseMetaDataTracker.isAttemptRestore());
        return response;
    }

    @RequestMapping(value = {Constants.URL_DELETE_APPLICATION_DBS, Constants.URL_UPDATE}, method = RequestMethod.POST)
    @UserLock
    public NotificationMessage deleteApplicationDbs(
            @RequestBody DeleteApplicationDbsRequestBean deleteRequest,
            HttpServletRequest request) {

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
        notificationLogger.logNotification(notificationMessage, request);
        return notificationMessage;
    }

    @RequestMapping(value = Constants.URL_CLEAR_USER_DATA, method = RequestMethod.POST)
    @UserLock
    public NotificationMessage clearUserData(
            @RequestBody AuthenticatedRequestBean requestBean,
            HttpServletRequest request) throws InvalidStructureException {

        String message = "Successfully cleared the user data for  " + requestBean.getUsername();
        new UserDB(
                requestBean.getDomain(),
                requestBean.getUsername(),
                requestBean.getRestoreAs()
        ).deleteDatabaseFolder();
        NotificationMessage notificationMessage = new NotificationMessage(message, false, NotificationMessage.Tag.clear_data);
        notificationLogger.logNotification(notificationMessage, request);
        CaseSearchDB caseSearchDB = new CaseSearchDB(requestBean.getDomain(), requestBean.getUsername(),
                requestBean.getRestoreAs());
        caseSearchDB.deleteDatabaseFile();
        return notificationMessage;
    }

    @RequestMapping(value = Constants.URL_CHECK_LOCKS, method = RequestMethod.POST)
    public LockReportBean checkLocks(@RequestBody AuthenticatedRequestBean requestBean) throws Exception {
        String key = LockAspect.getLockKeyForAuthenticatedBean(requestBean, formSessionService);


        Integer secondsLocked = userLockRegistry.getTimeLocked(key);
        if(secondsLocked == null) {
            return new LockReportBean(false, 0);
        } else{
            return new LockReportBean(true, secondsLocked);
        }
    }

    @RequestMapping(value = Constants.URL_BREAK_LOCKS, method = RequestMethod.POST)
    public NotificationMessage breakLocks(@RequestBody AuthenticatedRequestBean requestBean) throws Exception {
        String key = LockAspect.getLockKeyForAuthenticatedBean(requestBean, formSessionService);

        String message;

        if(userLockRegistry.breakAnyExistingLocks(key)) {
            message = "A lock existed and it was requested to be evicted";
        } else {
            message = "No locks for the current user";
        }

        return new NotificationMessage(message, false, NotificationMessage.Tag.break_locks);
    }

    @RequestMapping(value = Constants.URL_SERVER_UP, method = RequestMethod.GET)
    public ServerUpBean serverUp() throws Exception {
        return new ServerUpBean();
    }

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
        } catch (XFormParseException | XPathException xfpe) {
            reporter.setFailed(xfpe);
        } catch (Exception e) {
            log.error("Validate Form threw exception", e);
            reporter.setFailed(e);
        }

        return reporter.generateJSONReport();
    }
}
