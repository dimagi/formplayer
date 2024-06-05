package org.commcare.formplayer.web.client;

import org.commcare.formplayer.util.RequestUtils;
import org.springframework.http.HttpRequest;

/**
 * Filter to determine if a request is a request to CommCare. Additionally, filter based on
 * whether the current Spring request was authenticated with HMAC or not.
 */
public class CommCareHmacRequestFilter extends CommCareRequestFilter {

    private final boolean matchHmac;

    public CommCareHmacRequestFilter(String commcareHost, boolean matchHmac) {
        super(commcareHost);
        this.matchHmac = matchHmac;
    }

    public boolean isMatch(HttpRequest request) {
        return super.isMatch(request) && matchHmac == RequestUtils.requestAuthedWithHmac();
    }
}
