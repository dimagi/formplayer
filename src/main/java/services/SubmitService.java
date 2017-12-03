package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import util.Constants;
import util.SimpleTimer;

/**
 * Service that handles form submission to CommCareHQ
 */
public class SubmitService {

    @Autowired
    RestoreFactory restoreFactory;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    private CategoryTimingHelper.RecordingTimer submitTimer;

    public ResponseEntity<String> submitForm(String formXml, String submitUrl) {
        submitTimer = categoryTimingHelper.newTimer(Constants.TimingCategories.SUBMIT_FORM_TO_HQ);
        submitTimer.start();
        try {
            RestTemplate restTemplate = new RestTemplate();
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
}
