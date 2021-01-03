package org.commcare.formplayer.objects;

import com.fasterxml.jackson.annotation.*;

import java.util.Hashtable;

/**
 * Created by jschweers on 12/28/20.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryData {

    private Hashtable<String, Hashtable<String, String>> inputs = new Hashtable<>();
    private Hashtable<String, Boolean> execute = new Hashtable<>();

    @JsonGetter(value="inputs")
    public Hashtable<String, Hashtable<String, String>> getInputs() {
        return inputs;
    }

    @JsonSetter(value="inputs")
    public void setData(Hashtable<String, Hashtable<String, String>> inputs) {
        this.inputs = inputs;
    }

    @JsonGetter(value="execute")
    public Hashtable<String, Boolean> getExecute() {
        return execute;
    }

    @JsonSetter(value="execute")
    public void setExecute(Hashtable<String, Boolean> execute) {
        this.execute = execute;
    }
}
