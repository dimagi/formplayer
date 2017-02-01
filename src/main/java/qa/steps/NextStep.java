package qa.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;

/**
 * Created by willpride on 2/1/17.
 */
public class NextStep implements StepDefinition {
    @Override
    public String getRegularExpression() {
        return "^Next$";
    }

    @Override
    public JSONObject getPostBody(JSONObject last, String[] args) throws JsonProcessingException {
        return new JSONObject();
    }

    @Override
    public String getUrl() {
        return "next_index";
    }
}
