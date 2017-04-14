package services.impl;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import services.AuthService;
import services.FormattedQuestionsService;

/**
 * Implementation of getting the formatted questions by requesting HQ to generate
 * template
 */
public class FormattedQuestionsServiceImpl implements FormattedQuestionsService {
    @Value("${commcarehq.host}")
    private String host;

    @Autowired
    private AuthService authService;

    @Override
    public QuestionResponse getFormattedQuestions(String domain, String appId, String xmlns, String instanceXml) {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();

        body.add("instanceXml", instanceXml);
        body.add("xmlns", xmlns);
        body.add("appId", appId);

        HttpEntity<?> entity = new HttpEntity<Object>(body, authService.getAuth().getAuthHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                getFormattedQuestionsUrl(host, domain),
                HttpMethod.POST,
                entity,
                String.class
        );
        if (response.getStatusCode().value() == 200) {
            String responseBody = response.getBody();
            JSONObject responseJSON = new JSONObject(responseBody);
            return new QuestionResponse(
                    responseJSON.getString("form_data"),
                    responseJSON.getJSONArray("form_questions")
            );
        } else {
            throw new RuntimeException("Error fetching debugging context");
        }
    }

    private String getFormattedQuestionsUrl(String host, String domain) {
        return host + "/a/" + domain + "/cloudcare/api/readable_questions/";
    }
}
