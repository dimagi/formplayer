package org.commcare.formplayer.screens;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.RemoteRequestEntry;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.SyncScreen;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Screen to make a sync request to HQ after a case claim
 */
public class FormplayerSyncScreen extends SyncScreen {

    private String asUser;
    private String builtQuery;
    private String url;

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
            setUrl(syncPost.getUrl().toString());
            Hashtable<String, String> params = syncPost.getEvaluatedParams(sessionWrapper.getEvaluationContext());
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            // hack because HQ isn't accepting the first query param key properly
            addQueryParam(builder, "buffer", "buffer");
            if (asUser != null) {
                addQueryParam(builder, "commcare_login_as", asUser);
            }
            for (String key: params.keySet()){
                addQueryParam(builder, key, params.get(key));
            }
            builder.build(true);
            builtQuery = builder.toUriString();
        } else {
            // expected a sync entry; clear session and show vague 'session error' message to user
            throw new RuntimeException("Initialized sync request while not on sync screen");
        }
    }

    private void addQueryParam(UriComponentsBuilder builder, String name, String value) {
        try {
            builder.queryParam(name, URLEncoder.encode(value, UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not encode query parameter " + name + ": " + value);
        }
    }

    public String getBuiltQuery() {
        return builtQuery;
    }

    public void setBuiltQuery(String builtQuery) {
        this.builtQuery = builtQuery;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
