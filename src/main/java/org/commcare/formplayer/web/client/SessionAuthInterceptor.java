package org.commcare.formplayer.web.client;

import lombok.extern.apachecommons.CommonsLog;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

/**
 * Rest request interceptor that will add Django session auth headers to requests that require it
 */
@CommonsLog
public class SessionAuthInterceptor extends CommCareAuthInterceptor {

    public SessionAuthInterceptor(CommCareRequestFilter requestFilter) {
        super(requestFilter);
    }

    @Override
    protected HttpRequest modifyRequest(HttpRequest request, byte[] body) {
        HttpHeaders sessionHeaders = getSessionHeaders();
        request.getHeaders().addAll(sessionHeaders);
        return request;
    }

    public HttpHeaders getSessionHeaders() {
        HttpHeaders headers = new HttpHeaders();
        RequestUtils.getUserDetails().ifPresent(userDetails -> {
            String authToken = userDetails.getAuthToken();
            String auth = Constants.POSTGRES_DJANGO_SESSION_ID + "=" + authToken;
            headers.add("Cookie", auth);
            headers.add(Constants.POSTGRES_DJANGO_SESSION_ID, authToken);
            headers.add("Authorization", auth);
        });
        return headers;
    }

}
