package qa.steps;

import beans.AnswerQuestionRequestBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import qa.TestFailException;
import qa.TestState;
import util.Constants;

/**
 * Created by willpride on 2/1/17.
 */
public class AnswerStep implements StepDefinition {

    @Override
    public String getRegularExpression() {
        return "I enter text (.*)";
    }

    @Override
    public JSONObject getPostBody(JSONObject last, TestState currentState, String[] args) throws JsonProcessingException {
        String answer = args[0];
        AnswerQuestionRequestBean request = new AnswerQuestionRequestBean();
        request.setAnswer(answer);
        JSONObject json = new JSONObject(new ObjectMapper().writeValueAsString(request));
        return json;
    }

    @Override
    public String getUrl() {
        return Constants.URL_ANSWER_QUESTION;
    }

    @Override
    public void doWork(JSONObject lastResponse, TestState currentState, String[] args) throws TestFailException {

    }
}
