package org.commcare.formplayer.screens;

import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.util.screen.QueryScreen;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URL;

import com.google.common.collect.*;

/**
 * Created by willpride on 8/7/16.
 */
public class FormplayerQueryScreen extends QueryScreen {

    public FormplayerQueryScreen() {
        super(null, null, null);
    }

    /**
     * @param skipDefaultPromptValues don't apply the default value expressions for query prompts
     * @return case search url with search prompt values
     */
    public URI getUri(boolean skipDefaultPromptValues) {
        URL url = getBaseUrl();
        Multimap<String, String> queryParams = getQueryParams(skipDefaultPromptValues);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url.toString());
        for (String key : queryParams.keySet()) {
            QueryPrompt prompt = userInputDisplays.get(key);
            for (String value : queryParams.get(key)) {
                if (prompt != null && prompt.isSelect()) {
                    String[] selectedChoices = RemoteQuerySessionManager.extractSelectChoices(value);
                    for (String selectedChoice : selectedChoices) {
                        builder.queryParam(key, selectedChoice);
                    }
                } else {
                    builder.queryParam(key, value);
                }
            }
        }
        return builder.build().toUri();
    }
}
