package org.commcare.formplayer.objects;

import com.fasterxml.jackson.annotation.*;

import java.util.Hashtable;
import java.util.Map;


/**
 * Created by jschweers on 12/28/20.
 *
 * QueryData stores the case search & claim data for a session.
 * It's a hashtable keyed by command id, e.g., "search_command.m2"
 * For each command, QueryData stores two values:
 * 1. A boolean "execute" flag. If true, the search should be run.
 * If not, just fetch the current values of the prompts.
 * 2. An "inputs" map where keys are field names and values are
 * search terms.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryData extends Hashtable<String, Object> {
    private String KEY_EXECUTE = "execute";
    private String KEY_INPUTS = "inputs";

    public Boolean getExecute(String key) {
        Map<String, Object> value = (Map<String, Object>)this.get(key);
        if (value != null) {
            return (Boolean)value.get(this.KEY_EXECUTE);
        }
        return new Boolean(false);
    }

    public void setExecute(String key, Boolean value) {
        this.initKey(key);
        ((Map<String, Object>)this.get(key)).put(this.KEY_EXECUTE, value);
    }

    public Hashtable<String, String> getInputs(String key) {
        Map<String, Object> value = (Map<String, Object>)this.get(key);
        if (value != null) {
            Map<String, String> valueInputs = (Map<String, String>)value.get(this.KEY_INPUTS);
            if (valueInputs != null) {
                Hashtable<String, String> inputs = new Hashtable<String, String>();
                for (String inputKey : valueInputs.keySet()) {
                    inputs.put(inputKey, (String)valueInputs.get(inputKey));
                }
                return inputs;
            }
        }
        return null;
    }

    public void setInputs(String key, Hashtable<String, String> value) {
        this.initKey(key);
        ((Map<String, Object>)this.get(key)).put(this.KEY_INPUTS, value);
    }

    private void initKey(String key) {
        if (this.get(key) == null) {
            this.put(key, new Hashtable<String, Object>());
        }
    }
}
