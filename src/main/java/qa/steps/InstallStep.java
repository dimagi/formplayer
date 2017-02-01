package qa.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;

/**
 * Created by willpride on 2/1/17.
 */
public class InstallStep implements StepDefinition {
    @Override
    public String getRegularExpression() {
        return "^I install the app with id (.*)$";
    }

    @Override
    public JSONObject getPostBody(JSONObject lastResponse, String[] args) throws JsonProcessingException {
        return null;
    }


    @Override
    public String getUrl() {
        return null;
    }
}
