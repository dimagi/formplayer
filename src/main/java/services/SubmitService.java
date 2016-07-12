package services;

import auth.HqAuth;
import org.springframework.http.ResponseEntity;

/**
 * Service that handles form submission to CommCareHQ
 */
public interface SubmitService {
    ResponseEntity<String> submitForm(String formXml, String submitUrl, HqAuth auth);
}
