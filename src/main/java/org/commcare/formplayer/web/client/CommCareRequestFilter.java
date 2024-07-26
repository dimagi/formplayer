package org.commcare.formplayer.web.client;

import org.commcare.formplayer.util.RequestUtils;
import org.springframework.http.HttpRequest;

import java.util.function.Predicate;

/**
 * Filter to determine if a request is a request to CommCare.
 */
public class CommCareRequestFilter {

    private final String commcareHost;

    public CommCareRequestFilter(String commcareHost) {
        this.commcareHost = commcareHost;
    }

    public boolean isMatch(HttpRequest request) {
        return request.getURI().toString().startsWith(commcareHost);
    }
}
