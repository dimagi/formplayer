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
import util.Constants;
import util.SimpleTimer;

import java.io.IOException;

/**
 * Service that handles form submission to CommCareHQ
 */
public class SubmitService extends DefaultResponseErrorHandler {

    @Autowired
    RestoreFactory restoreFactory;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    private final Log log = LogFactory.getLog(SubmitService.class);

    private CategoryTimingHelper.RecordingTimer submitTimer;

    public ResponseEntity<String> submitForm(String formXml, String submitUrl) {
        submitTimer = categoryTimingHelper.newTimer(Constants.TimingCategories.SUBMIT_FORM_TO_HQ);
        submitTimer.start();
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setErrorHandler(this);
            HttpEntity<?> entity = new HttpEntity<Object>(formXml, restoreFactory.getUserHeaders());
            return restTemplate.exchange(submitUrl,
                    HttpMethod.POST,
                    entity, String.class);
        } finally {
            submitTimer.end().record();
        }
    }

    public SimpleTimer getSubmitTimer() {
        return submitTimer;
    }

    // Overriding the default error handler allows us to perform error handling in FormController
    // rather than at the Spring level
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        log.error("Error submitting form: " + response);
    }
}
