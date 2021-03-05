package org.commcare.formplayer.services;

import org.commcare.formplayer.web.client.WebClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
    private WebClient webclient;

    public QuestionResponse getFormattedQuestions(String domain, String appId, String xmlns, String instanceXml) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();

        body.add("instanceXml", instanceXml);
        body.add("xmlns", xmlns);
        body.add("appId", appId);

        String responseBody = webclient.post(
                getFormattedQuestionsUrl(host, domain), body, restoreFactory.getUserHeaders()
        );
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
