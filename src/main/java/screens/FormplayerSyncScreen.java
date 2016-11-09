package screens;

import auth.HqAuth;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.RemoteRequestEntry;
import org.commcare.util.screen.SyncScreen;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Hashtable;

/**
 * Screen to make a sync request to HQ after a case claim
 */
public class FormplayerSyncScreen extends SyncScreen {
    SessionWrapper sessionWrapper;

    public ResponseEntity<String> launchRemoteSync(HqAuth auth){
        String command = sessionWrapper.getCommand();
        Entry commandEntry = sessionWrapper.getPlatform().getEntry(command);
        if (commandEntry instanceof RemoteRequestEntry) {
            PostRequest syncPost = ((RemoteRequestEntry)commandEntry).getPostRequest();
            Hashtable<String, String> params = syncPost.getEvaluatedParams(sessionWrapper.getEvaluationContext());
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            // hack because HQ isn't accepting the first query param key properly
            builder.queryParam("buffer", "buffer");
            HttpHeaders headers = auth.getAuthHeaders();
            for(String key: params.keySet()){
                builder.queryParam(key, params.get(key));
            }
            String builtQuery = builder.toUriString();
            builder.build();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
            HttpEntity<String> entity = new HttpEntity<>(builtQuery, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response =
                    restTemplate.exchange(syncPost.getUrl().toString(),
                            HttpMethod.POST,
                            entity, String.class);
            return response;
        } else {
            // expected a sync entry; clear session and show vague 'session error' message to user
            return null;
        }
    }
}
