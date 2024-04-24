package org.commcare.formplayer.web.client;

import org.commcare.formplayer.util.RequestUtils;
import org.springframework.http.HttpRequest;

import java.util.function.Predicate;

/**
 * Filter to determine if a request is a request to CommCare. Additionally, it can filter based on
 * whether the current Spring request was authenticated with HMAC.
 */
public class CommCareRequestFilter implements Predicate<HttpRequest> {

    private final String commcareHost;
    private final boolean matchHmac;

    public CommCareRequestFilter(String commcareHost, boolean matchHmac) {
        this.commcareHost = commcareHost;
        this.matchHmac = matchHmac;
    }

    @Override
    public boolean test(HttpRequest request) {
        boolean currentRequestUsedHMAC = RequestUtils.requestAuthedWithHmac();
        boolean isCommCareRequest = request.getURI().toString().startsWith(commcareHost);
        return isCommCareRequest && (matchHmac == currentRequestUsedHMAC);
    }
}
