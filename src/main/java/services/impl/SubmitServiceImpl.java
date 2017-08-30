package services.impl;

import auth.HqAuth;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import services.RestoreFactory;
import services.SubmitService;
import util.Constants;

/**
 * RestTemplate implementation for submitting forms to HQ
 */
public class SubmitServiceImpl implements SubmitService {

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    protected RestoreFactory restoreFactory;

    @Override
    public ResponseEntity<String> submitForm(String formXml, String submitUrl, HqAuth auth) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<Object>(formXml, auth.getAuthHeaders());
        long start = System.currentTimeMillis();
        ResponseEntity<String> response =
                restTemplate.exchange(submitUrl,
                        HttpMethod.POST,
                        entity, String.class);
        long taken = System.currentTimeMillis() - start;
        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_TIMINGS,
                taken,
                "domain:" + restoreFactory.getDomain(),
                "username" + restoreFactory.getWrappedUsername(),
                "request:" + "submit-form"
        );
        return response;
    }
}
