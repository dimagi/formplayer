package org.commcare.formplayer.auth;

import org.springframework.http.HttpHeaders;
import org.commcare.formplayer.util.Constants;

/**
 * Class for storing a Django auth key and returning its respective headers
 */
public class DjangoAuth implements HqAuth {

    private final String authKey;

    public DjangoAuth(String authKey) {
        this.authKey = authKey;
    }


    // We seem to need all of these headers at different times. TODO WSP figure that out
    @Override
    public HttpHeaders getAuthHeaders() {
        return new HttpHeaders(){
            {
                add("Cookie",  Constants.POSTGRES_DJANGO_SESSION_ID + "=" + authKey);
                add(Constants.POSTGRES_DJANGO_SESSION_ID,  authKey);
                add("Authorization",  Constants.POSTGRES_DJANGO_SESSION_ID + "=" + authKey);
            }
        };
    }

    @Override
    public String toString(){
        return "DjangoAuth key=" + authKey;
    }
}