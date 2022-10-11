package org.commcare.formplayer.web.client;

import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import okhttp3.OkHttpClient;

/**
 * Custom OkHttp request factor that disables retries on connection failures.
 */
public class FormplayerOkHttp3ClientHttpRequestFactory extends OkHttp3ClientHttpRequestFactory {

    public FormplayerOkHttp3ClientHttpRequestFactory() {
        super(new OkHttpClient.Builder().retryOnConnectionFailure(false).build());
    }
}
