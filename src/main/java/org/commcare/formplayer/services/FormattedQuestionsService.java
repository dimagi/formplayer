package org.commcare.formplayer.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Service that gets HTML formatted questions to display to the user
 * Implemented by by requesting HQ to generate template
 */
public class FormattedQuestionsService {

    @Autowired
    RestoreFactory restoreFactory;

    public class QuestionResponse {
        private String formattedQuestions;
        private JSONArray questionList;

        private QuestionResponse(String formattedQuestions, JSONArray questionList) {
            this.formattedQuestions = formattedQuestions;
            this.questionList = questionList;
        }
        public String getFormattedQuestions() {
            return formattedQuestions;
        }

        public JSONArray getQuestionList() {
            return questionList;
        }
    }
    @Value("${commcarehq.host}")
    private String host;

    @Autowired
    private RestTemplate restTemplate;

    public QuestionResponse getFormattedQuestions(String domain, String appId, String xmlns, String instanceXml) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();

        body.add("instanceXml", instanceXml);
        body.add("xmlns", xmlns);
        body.add("appId", appId);

        HttpEntity<?> entity = new HttpEntity<Object>(body, restoreFactory.getUserHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                getFormattedQuestionsUrl(host, domain),
                HttpMethod.POST,
                entity,
                String.class
        );
        String responseBody = response.getBody();
        JSONObject responseJSON = new JSONObject(responseBody);
        return new QuestionResponse(
                responseJSON.getString("form_data"),
                responseJSON.getJSONArray("form_questions")
        );
    }

    private String getFormattedQuestionsUrl(String host, String domain) {
        return host + "/a/" + domain + "/cloudcare/api/readable_questions/";
    }
}
