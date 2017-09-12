package services;

import auth.HqAuth;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Service that handles form submission to CommCareHQ
 */
public class SubmitService {

    public ResponseEntity<String> submitForm(String formXml, String submitUrl, HqAuth auth) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<Object>(formXml, auth.getAuthHeaders());
        ResponseEntity<String> response =
                restTemplate.exchange(submitUrl,
                        HttpMethod.POST,
                        entity, String.class);
        return response;
    }
}
