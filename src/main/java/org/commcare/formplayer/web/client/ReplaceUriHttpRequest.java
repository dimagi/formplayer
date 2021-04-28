package org.commcare.formplayer.web.client;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;

import java.net.URI;

public class ReplaceUriHttpRequest extends HttpRequestWrapper {

    private final URI uri;

    ReplaceUriHttpRequest(URI uri, HttpRequest request) {
        super(request);
        this.uri = uri;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }
}
