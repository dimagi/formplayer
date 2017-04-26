package services;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Created by willpride on 4/26/17.
 */
public interface SyncRequester {
    ResponseEntity<String> makeSyncRequest(String url, String params, HttpHeaders headers);
}
