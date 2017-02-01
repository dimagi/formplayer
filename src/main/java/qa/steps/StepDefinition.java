package qa.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;
import qa.TestFailException;
import qa.TestState;

/**
 * Created by willpride on 2/1/17.
 */
public interface StepDefinition {
    String getRegularExpression();
    JSONObject getPostBody(JSONObject lastResponse, TestState currentState, String[] args) throws JsonProcessingException;
    String getUrl();
    void doWork(JSONObject lastResponse, TestState currentState, String[] args) throws TestFailException;
}
