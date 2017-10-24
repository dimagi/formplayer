package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Service that handles form submission to CommCareHQ
 */
public class SubmitService {

    @Autowired
    RestoreFactory restoreFactory;

    public ResponseEntity<String> submitForm(String formXml, String submitUrl) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<Object>(formXml, restoreFactory.getUserHeaders());
        ResponseEntity<String> response =
                restTemplate.exchange(submitUrl,
                        HttpMethod.POST,
                        entity, String.class);
        return response;
    }
}
