package org.commcare.formplayer.web.client;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Rest request interceptor that will modify requests to CommCare with the appropriate authentication details.
 */
public abstract class CommCareAuthInterceptor implements ClientHttpRequestInterceptor {

    private final CommCareRequestFilter requestFilter;

    public CommCareAuthInterceptor(CommCareRequestFilter requestFilter) {
        this.requestFilter = requestFilter;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        if (requestFilter.isMatch(request)) {
            request = modifyRequest(request, body);
        }
        return execution.execute(request, body);
    }

    protected abstract HttpRequest modifyRequest(HttpRequest request, byte[] body);
}
