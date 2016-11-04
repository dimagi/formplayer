package beans.debugger;

import org.json.JSONObject;

/**
 * Created by benrudolph on 11/4/16.
 */
public class QuestionResponseItem {
    private String value;
    private String label;
    private String type;

    public QuestionResponseItem(JSONObject questionJSON) {
        this.value = questionJSON.getString("value");
        this.label = questionJSON.getString("label");
        this.type = questionJSON.getString("type");
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
