package services;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import beans.auth.HqSessionKeyBean;
import beans.auth.HqUserDetailsBean;
import exceptions.SessionAuthUnavailableException;
import exceptions.UserDetailsException;
import util.Constants;
import util.RequestUtils;

@Service
public class HqUserDetailsService {
    private final Log log = LogFactory.getLog(HqUserDetailsService.class);
    private final RestTemplate restTemplate;
    private final RestTemplateBuilder builder;

    @Value("${commcarehq.host}")
    private String host;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Autowired
    private ObjectMapper objectMapper;

    public HqUserDetailsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.builder = null;
    }

    public HqUserDetailsService(RestTemplateBuilder builder) {
        this.builder = builder;
        this.restTemplate = null;
    }

    public HqUserDetailsBean getUserDetails(String domain, String sessionKey) {
        HttpHeaders headers = new HttpHeaders();
        String data = null;
        try {
            data = objectMapper.writeValueAsString(new HqSessionKeyBean(domain, sessionKey));
            headers.set("X-MAC-DIGEST", getHmac(data));
        } catch (Exception e) {
            throw new UserDetailsException(e);
        }
        HttpEntity<String> request = new HttpEntity<>(data, headers);
        RestTemplate template = builder == null ? restTemplate : builder.build();
        template.setErrorHandler(new DefaultResponseErrorHandler()  {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if(response.getRawStatusCode() == 404) {
                    throw new SessionAuthUnavailableException();
                }
            }
        });
        HqUserDetailsBean userDetails = template.postForObject(getSessionDetailsUrl(), request, HqUserDetailsBean.class);
        return userDetails;
    }

    private String getSessionDetailsUrl() {
        return host + Constants.SESSION_DETAILS_VIEW;
    }

    private String getHmac(String data) throws Exception {
        return RequestUtils.getHmac(formplayerAuthKey, data);
    }
}
