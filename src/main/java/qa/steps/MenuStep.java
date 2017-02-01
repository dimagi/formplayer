package qa.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by willpride on 2/1/17.
 */
public class MenuStep implements StepDefinition {
    @Override
    public String getRegularExpression() {
        return "^I select (form|module) (.*)$";
    }

    @Override
    public JSONObject getPostBody(JSONObject lastResponse, String[] args) throws JsonProcessingException {
        String menuSelection = args[1];
        if(!lastResponse.has("commands")) {
            throw new RuntimeException("Tried to make selection " + menuSelection + " but not on menu");
        }
        JSONObject requestBody = new JSONObject();
        JSONArray commands = (JSONArray) lastResponse.get("commands");
        boolean matched = false;
        for (int i = 0; i < commands.length(); i++) {
            if((commands.get(i).toString()).contains(menuSelection)) {
                JSONArray selections = getSelections(lastResponse);
                selections.put(i);
                requestBody.put("selections", selections);
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new RuntimeException("Argument "  + menuSelection + " didn't match commands " + commands);
        }
        return requestBody;
    }

    private JSONArray getSelections(JSONObject lastResponse) throws JSONException {
        if (lastResponse.has("selections")) {
            return lastResponse.getJSONArray("selections");
        } else {
            return new JSONArray();
        }
    }

    @Override
    public String getUrl() {
        return "navigate_menu";
    }
}
