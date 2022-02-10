package org.commcare.formplayer.web.client;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Rest request interceptor that will replace the host and port of any request
 */
@CommonsLog
public class RewriteHostRequestInterceptor implements ClientHttpRequestInterceptor {

    private final String commcareHost;
    private final URI commcareUri;

    public RewriteHostRequestInterceptor(String commcareHost) throws URISyntaxException {
        this.commcareHost = commcareHost;
        this.commcareUri = new URI(commcareHost);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        URI uri = request.getURI();
        if (!uri.toString().startsWith(commcareHost)) {
            URI newUri = UriComponentsBuilder.fromUri(uri)
                    .scheme(commcareUri.getScheme())
                    .host(commcareUri.getHost())
                    .port(commcareUri.getPort())
                    .build().toUri();
            request = new ReplaceUriHttpRequest(newUri, request);
            log.warn(String.format("HTTP request to '%s' rewritten to '%s'", formatHost(uri), formatHost(newUri)));
        }
        return execution.execute(request, body);
    }

    private String formatHost(URI uri) {
        String port = uri.getPort() != -1 ? String.format(":%s", uri.getPort()) : "";
        return String.format("%s://%s%s", uri.getScheme(), uri.getHost(), port);
    }
}
