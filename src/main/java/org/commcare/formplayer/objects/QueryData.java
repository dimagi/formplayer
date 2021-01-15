package org.commcare.formplayer.objects;

import com.fasterxml.jackson.annotation.*;

import java.util.Hashtable;
import java.util.Map;


/**
 * Created by jschweers on 12/28/20.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryData {
    private String KEY_EXECUTE = "execute";
    private String KEY_INPUTS = "inputs";

    private Hashtable<String, Map<String, Object>> data = new Hashtable<>();

    @JsonGetter(value="data")
    public Hashtable<String, Map<String, Object>> getData() {
        return data;
    }

    @JsonSetter(value="data")
    public void setData(Hashtable<String, Map<String, Object>> data) {
        this.data = data;
    }

    public Boolean getExecute(String key) {
        Map<String, Object> value = this.data.get(key);
        if (value != null) {
            return (Boolean) value.get(this.KEY_EXECUTE);
        }
        return new Boolean(false);
    };

    public Hashtable<String, String> getInputs(String key) {
        Map<String, Object> value = this.data.get(key);
        if (value != null) {
            Map<String, String> valueInputs = (Map<String, String>) value.get(this.KEY_INPUTS);
            if (valueInputs != null) {
                Hashtable<String, String> inputs = new Hashtable<String, String>();
                for (String inputKey : valueInputs.keySet()) {
                    inputs.put(inputKey, (String) valueInputs.get(inputKey));
                }
                return inputs;
            }
        }
        return null;
    };
}
