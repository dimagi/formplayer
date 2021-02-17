package org.commcare.formplayer.application;

import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.beans.*;
import org.commcare.formplayer.objects.FormSessionListView;
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
@RestController
@EnableAutoConfiguration
public class IncompleteSessionController extends AbstractBaseController{

    private final Log log = LogFactory.getLog(IncompleteSessionController.class);

    @RequestMapping(value = Constants.URL_INCOMPLETE_SESSION , method = RequestMethod.POST)
    @UserLock
    @UserRestore
    public NewFormResponse openIncompleteForm(@RequestBody SessionRequestBean incompleteSessionRequestBean,
                                              @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        SerializableFormSession session = formSessionService.getSessionById(incompleteSessionRequestBean.getSessionId());
        storageFactory.configure(session);
        return newFormResponseFactory.getResponse(session);
    }

    @RequestMapping(value = Constants.URL_GET_SESSIONS, method = RequestMethod.POST)
    @UserRestore
    public GetSessionsResponse getSessions(@RequestBody AuthenticatedRequestBean getSessionRequest,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        String scrubbedUsername = TableBuilder.scrubName(getSessionRequest.getUsername());

        List<FormSessionListView> formplayerSessions = formSessionService.getSessionsForUser(
                scrubbedUsername, getSessionRequest.getDomain()
        );

        ArrayList<FormSessionListView> sessions = new ArrayList<>();
        Set<String> formplayerSessionIds = new HashSet<>();

        for (FormSessionListView serializableFormSession : formplayerSessions) {
            sessions.add(serializableFormSession);
            formplayerSessionIds.add(serializableFormSession.getId());
        }

        return new GetSessionsResponse(restoreFactory.getSqlSandbox().getCaseStorage(), sessions);
    }

    @RequestMapping(value = Constants.URL_DELETE_INCOMPLETE_SESSION , method = RequestMethod.POST)
    public NotificationMessage deleteIncompleteForm(
            @RequestBody SessionRequestBean incompleteSessionRequestBean,
            @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        deleteSession(incompleteSessionRequestBean.getSessionId());
        return new NotificationMessage("Successfully deleted incomplete form.", false, NotificationMessage.Tag.incomplete_form);
    }

    protected void deleteSession(String id) {
        formSessionService.deleteSessionById(id);
    }

}
