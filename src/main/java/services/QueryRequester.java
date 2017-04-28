package services;

import org.springframework.http.HttpHeaders;

/**
 * Created by willpride on 4/26/17.
 */
public interface QueryRequester {
    String makeQueryRequest(String uri, HttpHeaders headers);
}
