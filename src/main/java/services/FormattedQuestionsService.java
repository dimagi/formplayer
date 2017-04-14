package services;

import org.json.JSONArray;

/**
 * Service that gets HTML formatted questions to display to the user
 */
public interface FormattedQuestionsService {
    QuestionResponse getFormattedQuestions(String domain, String appId, String xmlns, String instanceXml);

    class QuestionResponse {
        private String formattedQuestions;
        private JSONArray questionList;

        public QuestionResponse(String formattedQuestions, JSONArray questionList) {
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
}
