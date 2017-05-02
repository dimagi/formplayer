package services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private final Log log = LogFactory.getLog(QueryRequesterImpl.class);

    @Override
    public String makeQueryRequest(String uri, HttpHeaders headers) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(uri,
                        HttpMethod.GET,
                        new HttpEntity<String>(headers),
                        String.class);
        String responseBody = response.getBody();
        log.info(String.format("Query request to URL %s returned result %s", uri, responseBody));
        return responseBody;
    }
}
