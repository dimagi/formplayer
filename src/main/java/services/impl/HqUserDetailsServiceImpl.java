package services.impl;

import beans.auth.HqSessionKeyBean;
import beans.auth.HqUserDetailsBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.UserDetailsException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import services.HqUserDetailsService;
import util.Constants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class HqUserDetailsServiceImpl implements HqUserDetailsService {
    private final Log log = LogFactory.getLog(HqUserDetailsServiceImpl.class);
    private final RestTemplate restTemplate;

    @Value("${commcarehq.host}")
    private String host;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Autowired
    private ObjectMapper objectMapper;

    public HqUserDetailsServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public HqUserDetailsServiceImpl(RestTemplateBuilder builder) {
        restTemplate = builder.build();
    }

    @Override
    public HqUserDetailsBean getUserDetails(String sessionKey) {
        HttpHeaders headers = new HttpHeaders();
        String data = null;
        try {
            data = objectMapper.writeValueAsString(new HqSessionKeyBean(sessionKey));
            headers.set("X_MAC_DIGEST", getHmac(data));
        } catch (Exception e) {
            throw new UserDetailsException(e);
        }
        HttpEntity<String> request = new HttpEntity<>(data, headers);
        HqUserDetailsBean userDetails = restTemplate.postForObject(getSessionDetailsUrl(), request, HqUserDetailsBean.class);
        log.info(userDetails.toString());
        return userDetails;
    }

    private String getSessionDetailsUrl() {
        return host + Constants.SESSION_DETAILS_VIEW;
    }

    private String getHmac(String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(formplayerAuthKey.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
    }
}
