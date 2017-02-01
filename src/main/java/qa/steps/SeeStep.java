package qa.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;
import qa.TestFailException;
import qa.TestState;

import java.util.Arrays;

/**
 * Created by willpride on 2/1/17.
 */
public class SeeStep implements StepDefinition {
    @Override
    public String getRegularExpression() {
        return "I see \"(.*)\"";
    }

    @Override
    public JSONObject getPostBody(JSONObject lastResponse, TestState currentState, String[] args) throws JsonProcessingException {
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public void doWork(JSONObject lastResponse, TestState currentState, String[] args) throws TestFailException {
        if(!lastResponse.toString().contains(args[0])) {
            throw new TestFailException(args[0] + " not found.");
        }
    }
}
