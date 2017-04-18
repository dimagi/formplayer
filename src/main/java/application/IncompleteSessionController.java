package application;

import annotations.UserLock;
import annotations.UserRestore;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.*;
import exceptions.FormNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.FormSessionRepo;
import session.FormSession;
import util.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @UserLock
    @UserRestore
    public NewFormResponse openIncompleteForm(@RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
                                              @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession session;
        try {
            session = formSessionRepo.findOneWrapped(incompleteSessionRequestBean.getSessionId());
        } catch(FormNotFoundException e) {
            session = migratedFormSessionRepo.findOneWrapped(incompleteSessionRequestBean.getSessionId());
            // Move over to formplayer db
            formSessionRepo.save(session);
            migratedFormSessionRepo.delete(incompleteSessionRequestBean.getSessionId());
        }
        return newFormResponseFactory.getResponse(session);
    }

    @ApiOperation(value = "Get a list of the current user's sessions")
    @RequestMapping(value = Constants.URL_GET_SESSIONS, method = RequestMethod.POST)
    @UserRestore
    public GetSessionsResponse getSessions(@RequestBody GetSessionsBean getSessionRequest,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        String scrubbedUsername = TableBuilder.scrubName(getSessionRequest.getUsername());

        // Old CloudCare doesn't use scrubbed usernames
        List<SerializableFormSession> migratedSessions = migratedFormSessionRepo.findUserSessions(
                getSessionRequest.getUsername());

        List<SerializableFormSession> formplayerSessions = formSessionRepo.findUserSessions(scrubbedUsername);

        ArrayList<SerializableFormSession> sessions = new ArrayList<>();
        Set<String> formplayerSessionIds = new HashSet<>();

        for (SerializableFormSession serializableFormSession : formplayerSessions) {
            sessions.add(serializableFormSession);
            formplayerSessionIds.add(serializableFormSession.getId());
        }

        if (migratedSessions.size() > 0) {
            for (SerializableFormSession migratedSession : migratedSessions) {
                // If we already have this session in the formplayer repo, skip it
                if (formplayerSessionIds.contains(migratedSession.getId())) {
                    continue;
                }
                SerializableFormSession serialSession = migratedSession;
                sessions.add(serialSession);
            }
        }

        return new GetSessionsResponse(restoreFactory.getSqlSandbox().getCaseStorage(), sessions);
    }

    @ApiOperation(value = "Delete an incomplete form session")
    @RequestMapping(value = Constants.URL_DELETE_INCOMPLETE_SESSION , method = RequestMethod.POST)
    public NotificationMessageBean deleteIncompleteForm(
            @RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
            @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        deleteSession(incompleteSessionRequestBean.getSessionId());
        return new NotificationMessageBean("Successfully deleted incomplete form.", false);
    }

    protected void deleteSession(String id) {
        formSessionRepo.delete(id);
        migratedFormSessionRepo.delete(id);
    }

}
