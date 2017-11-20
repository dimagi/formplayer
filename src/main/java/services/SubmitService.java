package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Service that handles form submission to CommCareHQ
 */
public class SubmitService extends DefaultResponseErrorHandler {

    @Autowired
    RestoreFactory restoreFactory;

    public ResponseEntity<String> submitForm(String formXml, String submitUrl) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(this);
        HttpEntity<?> entity = new HttpEntity<Object>(formXml, restoreFactory.getUserHeaders());
        return restTemplate.exchange(submitUrl,
                HttpMethod.POST,
                entity, String.class);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        restoreFactory.rollback();
    }
}
