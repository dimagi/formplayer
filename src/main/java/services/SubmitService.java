package services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private final Log log = LogFactory.getLog(SubmitService.class);

    public ResponseEntity<String> submitForm(String formXml, String submitUrl) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(this);
        HttpEntity<?> entity = new HttpEntity<Object>(formXml, restoreFactory.getUserHeaders());
        return restTemplate.exchange(submitUrl,
                HttpMethod.POST,
                entity, String.class);
    }

    // Overriding the default error handler allows us to perform error handling in FormController
    // rather than at the Spring level
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        log.error("Error submitting form: " + response);
    }
}
