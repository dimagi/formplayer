package requests;

import auth.HqAuth;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Created by willpride on 1/20/16.
 */
public class FormRequest {

    public FormRequest(){
    }

    public String getFormXml(String formUrl, HqAuth hqAuth){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(formUrl,
                        HttpMethod.GET,
                        new HttpEntity<String>(hqAuth.getAuthHeaders()), String.class);
        return response.getBody();
    }
}
