package services.impl;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import services.QueryRequester;

/**
 * Created by willpride on 4/26/17.
 */
public class QueryRequesterImpl implements QueryRequester {
    @Override
    public String makeQueryRequest(String uri, HttpHeaders headers) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(uri,
                        HttpMethod.GET,
                        new HttpEntity<String>(headers),
                        String.class);
        return response.getBody();
    }
}
