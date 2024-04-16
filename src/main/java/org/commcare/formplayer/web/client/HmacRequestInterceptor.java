package org.commcare.formplayer.web.client;

import lombok.extern.apachecommons.CommonsLog;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.util.RequestUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Rest request interceptor that will add HMAC headers to requests that require it
 */
@CommonsLog
public class HmacRequestInterceptor implements ClientHttpRequestInterceptor {

    private String formplayerAuthKey;


    public HmacRequestInterceptor(String formplayerAuthKey) throws URISyntaxException {
        this.formplayerAuthKey = formplayerAuthKey;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        if (RequestUtils.requestAuthedWithHmac()) {
            request = addAsParamIfNotPresent(request);
            HttpHeaders hmacHeaders = getHmacHeaders(request, body);
            request.getHeaders().addAll(hmacHeaders);
        }
        return execution.execute(request, body);
    }

    /**
     * HMAC requests require the 'as' parameter to be present in the request URI
     * since it is not possible to determine the user from the authentication header.
     */
    @NotNull
    private static HttpRequest addAsParamIfNotPresent(HttpRequest request) {
        URI uri = request.getURI();

        String rawQuery = uri.getRawQuery();
        boolean asParamMissing = rawQuery == null || Arrays.stream(rawQuery.split("&"))
                .noneMatch(param -> param.startsWith("as="));
        Optional<HqUserDetailsBean> userDetails = RequestUtils.getUserDetails();
        if (asParamMissing && userDetails.isPresent()) {
            String asParamValue = userDetails.get().getUsername();
            URI newUri = UriComponentsBuilder.fromUri(uri)
                    .queryParam("as", asParamValue)
                    .build().toUri();
            request = new ReplaceUriHttpRequest(newUri, request);
            log.warn(String.format("HMAC request augmented with 'as=%s' param", asParamValue));
        }
        return request;
    }

    private HttpHeaders getHmacHeaders(HttpRequest request, byte[] body) {
        try {
            return switch (Objects.requireNonNull(request.getMethod())) {
                case GET -> getHmacHeaderForGetRequest(request.getURI());
                case POST -> getHmacHeader(body);
                default -> throw new RuntimeException("Unsupported HTTP method: " + request.getMethod());
            };
        } catch (Exception e) {
            log.error("Could not get HMAC signature", e);
            throw new RuntimeException(e);
        }
    }

    private HttpHeaders getHmacHeaderForGetRequest(URI url) throws Exception {
        // Do HMAC auth which requires only the path and query components of the URL
        String requestPath = url.getRawPath();
        if (url.getRawQuery() != null) {
            requestPath = String.format("%s?%s", requestPath, url.getRawQuery());
        }

        return getHmacHeader(requestPath.getBytes(StandardCharsets.UTF_8));
    }

    private HttpHeaders getHmacHeader(byte[] data) throws Exception {
        String digest = RequestUtils.getHmac(formplayerAuthKey, data);
        return new HttpHeaders() {
            {
                add("X-MAC-DIGEST", digest);
            }
        };
    }
}
