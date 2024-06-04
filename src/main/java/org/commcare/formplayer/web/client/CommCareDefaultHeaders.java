package org.commcare.formplayer.web.client;

import org.commcare.formplayer.util.RequestUtils;
import org.javarosa.core.util.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateRequestCustomizer;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;


/**
 * Adds default headers to requests to CommCareHQ
 */
@Component
public class CommCareDefaultHeaders implements RestTemplateRequestCustomizer {

    private static final String ORIGIN_TOKEN_SLUG = "OriginToken";
    private final CommCareRequestFilter requestFilter;

    private ValueOperations<String, String> originTokens;

    @Autowired
    public CommCareDefaultHeaders(@Value("${commcarehq.host}") String commcareHost) {
        requestFilter = new CommCareRequestFilter(commcareHost);
    }

    @Resource(name = "redisTemplateString")
    public void setOriginTokens(ValueOperations<String, String> originTokens) {
        this.originTokens = originTokens;
    }

    @Override
    public void customize(ClientHttpRequest request) {
        if (!requestFilter.isMatch(request)) {
            return;
        }

        String ipAddress = RequestUtils.getIpAddress();
        if (ipAddress != null) {
            request.getHeaders().add("X-CommCareHQ-Origin-IP", ipAddress);
        }
        request.getHeaders().add("X-CommCareHQ-Origin-Token", getOriginTokenHeader());
    }

    private String getOriginTokenHeader() {
        String originToken = PropertyUtils.genUUID();
        String redisKey = String.format("%s%s", ORIGIN_TOKEN_SLUG, originToken);
        originTokens.set(redisKey, "valid", Duration.ofSeconds(60));
        return originToken;
    }
}
