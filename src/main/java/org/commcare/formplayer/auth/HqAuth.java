package org.commcare.formplayer.auth;

import org.springframework.http.HttpHeaders;

/**
 * Created by willpride on 1/13/16.
 */
public interface HqAuth {
    HttpHeaders getAuthHeaders();
}
