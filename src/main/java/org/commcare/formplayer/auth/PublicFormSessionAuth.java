package org.commcare.formplayer.auth;

import org.commcare.formplayer.util.Constants;
import org.springframework.http.HttpHeaders;

/**
 * {@link HqAuth} for a public web apps session.
 *
 * Emits the credential pair HQ requires to recognize a public session on its receiver/restore
 * endpoints: the {@code public_form_session_key} cookie carrying the session key together with the
 * {@code CommCare-Public-Session: true} header.
 */
public class PublicFormSessionAuth implements HqAuth {

    private final String sessionKey;

    public PublicFormSessionAuth(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    @Override
    public HttpHeaders getAuthHeaders() {
        return new HttpHeaders() {
            {
                add("Cookie", Constants.PUBLIC_FORM_SESSION_COOKIE_NAME + "=" + sessionKey);
                add(Constants.PUBLIC_FORM_SESSION_HEADER, Constants.PUBLIC_FORM_SESSION_HEADER_VALUE);
            }
        };
    }

    @Override
    public String toString() {
        return "PublicFormSessionAuth";
    }
}
