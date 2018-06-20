package services;

import beans.auth.HqSessionKeyBean;
import beans.auth.HqUserDetailsBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.UserDetailsException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import util.Constants;
import util.RequestUtils;

@Service
public class HqUserDetailsService {
    private final Log log = LogFactory.getLog(HqUserDetailsService.class);
    private final RestTemplate restTemplate;

    @Value("${commcarehq.host}")
    private String host;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Autowired
    private ObjectMapper objectMapper;

    public HqUserDetailsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public HqUserDetailsService(RestTemplateBuilder builder) {
        restTemplate = builder.build();
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
        HqUserDetailsBean userDetails = restTemplate.postForObject(getSessionDetailsUrl(), request, HqUserDetailsBean.class);
        return userDetails;
    }

    private String getSessionDetailsUrl() {
        return host + Constants.SESSION_DETAILS_VIEW;
    }

    private String getHmac(String data) throws Exception {
        return RequestUtils.getHmac(formplayerAuthKey, data);
    }
}
