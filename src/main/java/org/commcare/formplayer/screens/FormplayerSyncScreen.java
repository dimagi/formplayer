package org.commcare.formplayer.screens;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.RemoteRequestEntry;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.SyncScreen;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.util.Hashtable;

/**
 * Screen to make a sync request to HQ after a case claim
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
            Hashtable<String, String> params = syncPost.getEvaluatedParams(sessionWrapper.getEvaluationContext());
            queryParams = new LinkedMultiValueMap<String, String>();
            if (asUser != null) {

                queryParams.add("commcare_login_as", asUser);
            }
            for (String key: params.keySet()){
                queryParams.add(key, params.get(key));
            }
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
