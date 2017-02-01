package qa.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;
import qa.TestFailException;
import qa.TestState;

/**
 * Created by willpride on 2/1/17.
 */
public class NextStep implements StepDefinition {
    @Override
    public String getRegularExpression() {
        return "^Next$";
    }

    @Override
    public JSONObject getPostBody(JSONObject last, TestState currentState, String[] args) throws JsonProcessingException {
        return new JSONObject();
    }

    @Override
    public String getUrl() {
        return "next_index";
    }

    @Override
    public void doWork(JSONObject lastResponse, TestState currentState, String[] args) throws TestFailException {

    }
}
