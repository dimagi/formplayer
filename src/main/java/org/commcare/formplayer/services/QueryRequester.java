package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;


@Component
public class QueryRequester {

    @Autowired
    private RedisTemplate redisTemplateString;

    @Autowired
    private RestoreFactory restoreFactory;

    @Resource(name = "redisTemplateString")
    private ValueOperations<String, String> caseSearchCache;

    private final Log log = LogFactory.getLog(QueryRequester.class);

    public String makeQueryRequest(String uri, HttpHeaders headers) {
        String responseBody = loadResponseFromCache(uri);
        if (responseBody == null) {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(
                        // Spring framework automatically encodes urls. This ensures we don't pass in an already
                        // encoded url.
                        URLDecoder.decode(uri, "UTF-8"),
                        HttpMethod.GET,
                        new HttpEntity<String>(headers),
                        String.class
                );
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            responseBody = response.getBody();
            if (response.getStatusCode().is2xxSuccessful()) {
                cacheResponse(uri, responseBody);
            }
            log.info(String.format("Query request to URL %s successful", uri));
        }
        return responseBody;
    }

    private String loadResponseFromCache(String uri) {
        return caseSearchCache.get(getCacheKey(uri));
    }

    private void cacheResponse(String uri, String responseBody) {
        caseSearchCache.set(getCacheKey(uri), responseBody, 2, TimeUnit.MINUTES);
    }

    private String getCacheKey(String uri) {
        return restoreFactory.getScrubbedUsername() + "_" + uri;
    }
}
