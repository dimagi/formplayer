package org.commcare.formplayer.application;

import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.beans.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.database.TableBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import org.commcare.formplayer.util.Constants;

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

    private final Log log = LogFactory.getLog(IncompleteSessionController.class);

    @ApiOperation(value = "Open an incomplete form session")
    @RequestMapping(value = Constants.URL_INCOMPLETE_SESSION , method = RequestMethod.POST)
    @UserLock
    @UserRestore
    public NewFormResponse openIncompleteForm(@RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
                                              @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession session = formSessionRepo.findOneWrapped(incompleteSessionRequestBean.getSessionId());
        storageFactory.configure(session);
        return newFormResponseFactory.getResponse(session);
    }

    @ApiOperation(value = "Get a list of the current user's sessions")
    @RequestMapping(value = Constants.URL_GET_SESSIONS, method = RequestMethod.POST)
    @UserRestore
    public GetSessionsResponse getSessions(@RequestBody GetSessionsBean getSessionRequest,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        String scrubbedUsername = TableBuilder.scrubName(getSessionRequest.getUsername());

        List<SerializableFormSession> formplayerSessions = formSessionRepo.findUserSessions(scrubbedUsername);

        ArrayList<SerializableFormSession> sessions = new ArrayList<>();
        Set<String> formplayerSessionIds = new HashSet<>();

        for (SerializableFormSession serializableFormSession : formplayerSessions) {
            sessions.add(serializableFormSession);
            formplayerSessionIds.add(serializableFormSession.getId());
        }

        return new GetSessionsResponse(restoreFactory.getSqlSandbox().getCaseStorage(), sessions);
    }

    @ApiOperation(value = "Delete an incomplete form session")
    @RequestMapping(value = Constants.URL_DELETE_INCOMPLETE_SESSION , method = RequestMethod.POST)
    public NotificationMessage deleteIncompleteForm(
            @RequestBody IncompleteSessionRequestBean incompleteSessionRequestBean,
            @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        deleteSession(incompleteSessionRequestBean.getSessionId());
        return new NotificationMessage("Successfully deleted incomplete form.", false, NotificationMessage.Tag.incomplete_form);
    }

    protected void deleteSession(String id) {
        formSessionRepo.deleteById(id);
    }

}
