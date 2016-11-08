package application;

import auth.DjangoAuth;
import beans.*;
import exceptions.FormNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.database.TableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.FormSessionRepo;
import session.FormSession;
import util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Controller class (API endpoint) containing all incomplete session management commands
 *
 */
@Api(value = "Incomplete Session Controller", description = "Operations for navigating managing incomplete sessions")
@RestController
@EnableAutoConfiguration
public class IncompleteSessionController extends AbstractBaseController{

    @Value("${commcarehq.host}")
    private String host;

    private final Log log = LogFactory.getLog(IncompleteSessionController.class);

    @Autowired
    @Qualifier(value = "migrated")
    protected FormSessionRepo migratedFormSessionRepo;

    @ApiOperation(value = "Open an incomplete form session")
    @RequestMapping(value = Constants.URL_INCOMPLETE_SESSION , method = RequestMethod.POST)
    public NewFormResponse openIncompleteForm(@RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
                                              @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Incomplete session request with bean: " + incompleteSessionRequestBean + " sessionId :" + authToken);
        SerializableFormSession session;
        restoreFactory.configure(incompleteSessionRequestBean, new DjangoAuth(authToken));
        try {
            session = formSessionRepo.findOneWrapped(incompleteSessionRequestBean.getSessionId());
        } catch(FormNotFoundException e) {
            session = migratedFormSessionRepo.findOneWrapped(incompleteSessionRequestBean.getSessionId());
            // Move over to formplayer db
            formSessionRepo.save(session);
        }
        Lock lock = getLockAndBlock(session.getUsername());
        try {
            NewFormResponse response = newFormResponseFactory.getResponse(session);
            log.info("Return incomplete session response: " + response);
            return response;
        } finally {
            lock.unlock();
        }
    }
    @ApiOperation(value = "Get a list of the current user's sessions")
    @RequestMapping(value = Constants.URL_GET_SESSIONS, method = RequestMethod.POST)
    public GetSessionsResponse getSessions(@RequestBody GetSessionsBean getSessionRequest,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Get Session Request: " + getSessionRequest);

        String username = TableBuilder.scrubName(getSessionRequest.getUsername());

        restoreFactory.configure(getSessionRequest, new DjangoAuth(authToken));

        List<SerializableFormSession> migratedSessions = migratedFormSessionRepo.findUserSessions(
                getSessionRequest.getUsername());

        List<SerializableFormSession> formplayerSessions = formSessionRepo.findUserSessions(username);

        ArrayList<FormSession> formSessions = new ArrayList<>();

        for (int i = 0; i < formplayerSessions.size(); i++) {
            formSessions.add(new FormSession(formplayerSessions.get(i)));
        }

        if (migratedSessions.size() > 0) {

            // First, get the id of every session we  got from the Formplayer repo so we don't process duplicates
            ArrayList<String> formplayerSessionIds = new ArrayList<>();
            for (SerializableFormSession session: formplayerSessions) {
                formplayerSessionIds.add(session.getId());
            }

            for (int i = 0; i < migratedSessions.size(); i++) {
                // If we already have this session in the formplayer repo, skip it
                if (formplayerSessionIds.contains(migratedSessions.get(i).getId())) {
                    continue;
                }
                try {
                    SerializableFormSession serialSession = migratedSessions.get(i);
                    formSessions.add(new FormSession(serialSession));
                } catch (Exception e) {
                    // I think let's not crash on this.
                    log.error("Couldn't add session " + migratedSessions.get(i) + " with exception " + e);
                }
            }
        }

        return new GetSessionsResponse(formSessions);
    }

    @ApiOperation(value = "Delete an incomplete form session")
    @RequestMapping(value = Constants.URL_DELETE_INCOMPLETE_SESSION , method = RequestMethod.POST)
    public NotificationMessageBean deleteIncompleteForm(
            @RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
            @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        Lock lock = getLockAndBlock(incompleteSessionRequestBean.getUsername());
        try {
            deleteSession(incompleteSessionRequestBean.getSessionId());
            return new NotificationMessageBean("Successfully deleted incomplete form.", false);
        } finally {
            lock.unlock();
        }
    }

    protected void deleteSession(String id) {
        formSessionRepo.delete(id);
        migratedFormSessionRepo.delete(id);
    }

}
