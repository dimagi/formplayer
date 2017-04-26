package screens;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.RemoteRequestEntry;
import org.commcare.util.screen.SyncScreen;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Hashtable;

/**
 * Screen to make a sync request to HQ after a case claim
 */
public class FormplayerSyncScreen extends SyncScreen {

    private String asUser;
    private String builtQuery;
    private String url;

    public FormplayerSyncScreen(String asUser) {
        super();
        this.asUser = asUser;
    }

    @Override
    public void init (SessionWrapper sessionWrapper){

        super.init(sessionWrapper);
        String command = sessionWrapper.getCommand();
        Entry commandEntry = sessionWrapper.getPlatform().getEntry(command);
        if (commandEntry instanceof RemoteRequestEntry) {
            PostRequest syncPost = ((RemoteRequestEntry)commandEntry).getPostRequest();
            setUrl(syncPost.getUrl().toString());
            Hashtable<String, String> params = syncPost.getEvaluatedParams(sessionWrapper.getEvaluationContext());
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            // hack because HQ isn't accepting the first query param key properly
            builder.queryParam("buffer", "buffer");
            if (asUser != null) {
                builder.queryParam("commcare_login_as", asUser);
            }
            for(String key: params.keySet()){
                builder.queryParam(key, params.get(key));
            }
            builtQuery = builder.toUriString();
            builder.build();
        } else {
            // expected a sync entry; clear session and show vague 'session error' message to user
            throw new RuntimeException("Initialized sync request while not on sync screen");
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
