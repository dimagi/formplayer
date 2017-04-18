package qa.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONArray;
import org.json.JSONObject;
import qa.TestFailException;
import qa.TestState;

/**
 * Created by willpride on 2/1/17.
 */
public class MenuStep implements StepDefinition {
    @Override
    public String getRegularExpression() {
        return "I select (form|module) (.*)";
    }

    @Override
    public JSONObject getPostBody(JSONObject lastResponse, TestState currentState, String[] args) throws JsonProcessingException, TestFailException {
        String menuSelection = args[1];
        if(!lastResponse.has("commands")) {
            throw new TestFailException("Tried to make selection " + menuSelection + " but not on menu");
        }
        JSONObject requestBody = new JSONObject();
        JSONArray commands = (JSONArray) lastResponse.get("commands");
        boolean matched = false;
        for (int i = 0; i < commands.length(); i++) {
            if((commands.get(i).toString()).contains(menuSelection)) {
                currentState.addStep(""+i);
                requestBody.put("selections", currentState.getSteps());
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new TestFailException(String.format("Argument %s didn't match any commands %s", menuSelection, commands));
        }
        return requestBody;
    }

    @Override
    public String getUrl() {
        return "navigate_menu";
    }

    @Override
    public void doWork(JSONObject lastResponse, TestState currentState, String[] args) throws TestFailException {

    }
}
