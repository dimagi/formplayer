package beans.debugger;

import org.json.JSONObject;

/**
 * A class representing the question model returned from HQ. See HQ's
 * FormQuestionResponse class.
 */
public class AutoCompletableItem {
    protected String value;
    protected String label;
    protected String type;

    public AutoCompletableItem(String value, String label, String type) {
        this.value = value;
        this.label = label;
        this.type = type;
    }

    public AutoCompletableItem(JSONObject itemJSON) {
        this.value = itemJSON.getString("value");
        this.label = itemJSON.getString("label");
        this.type = itemJSON.getString("type");
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
