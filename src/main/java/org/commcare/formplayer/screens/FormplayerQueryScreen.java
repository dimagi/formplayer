package org.commcare.formplayer.screens;

import org.commcare.util.screen.QueryScreen;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URL;
import java.util.Hashtable;

/**
 * Created by willpride on 8/7/16.
 */
public class FormplayerQueryScreen extends QueryScreen {

    public FormplayerQueryScreen(){
        super(null, null, null);
    }

    public String getUriString() {
        URL url = getBaseUrl();
        Hashtable<String, String> params = getQueryParams();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url.toString());
        for(String key: params.keySet()){
            builder.queryParam(key, params.get(key));
        }
        return builder.toUriString();
    }
}
