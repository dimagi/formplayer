package org.commcare.formplayer.screens;

import com.google.common.collect.Multimap;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.RemoteRequestEntry;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.SyncScreen;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Screen to make a sync request to HQ after a case claim
 *
 * This ignores the OkHttpClient logic from SyncScreen.
 * Calling handleInputAndUpdateSession directly causes okhttp3.Credentials
 * to throw an error due to username being null. Instead of using handleInputAndUpdateSession
 * to execute the sync request, formplayer just grabs the url from this screen and then
 * posts is using WebClient - see MenuSessionRunnerService.doSync.
 */
public class FormplayerSyncScreen extends SyncScreen {

    private String asUser;
    private String url;
    private MultiValueMap<String, String> queryParams;

    public FormplayerSyncScreen(String asUser) {
        super(null, null, System.out);
        this.asUser = asUser;
    }

    @Override
    public void init (SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
        String command = sessionWrapper.getCommand();
        Entry commandEntry = sessionWrapper.getPlatform().getEntry(command);
        if (commandEntry instanceof RemoteRequestEntry) {
            PostRequest syncPost = ((RemoteRequestEntry)commandEntry).getPostRequest();
            url = syncPost.getUrl().toString();
            Multimap<String, String> params = syncPost.getEvaluatedParams(sessionWrapper.getEvaluationContext());
            queryParams = new LinkedMultiValueMap<String, String>();
            if (asUser != null) {
                queryParams.add("commcare_login_as", asUser);
            }
            params.forEach(queryParams::add);
        } else {
            // expected a sync entry; clear session and show vague 'session error' message to user
            throw new RuntimeException("Initialized sync request while not on sync screen");
        }
    }

    public MultiValueMap<String, String> getQueryParams() {
        return queryParams;
    }

    public String getUrl() {
        return url;
    }
}
