package qa.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;

/**
 * Created by willpride on 2/1/17.
 */
public interface StepDefinition {
    String getRegularExpression();
    JSONObject getPostBody(JSONObject lastResponse, String[] args) throws JsonProcessingException;
    String getUrl();
}
