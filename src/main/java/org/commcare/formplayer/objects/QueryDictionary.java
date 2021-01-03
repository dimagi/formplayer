package org.commcare.formplayer.objects;

import com.fasterxml.jackson.annotation.*;

import java.util.Hashtable;

/**
 * Created by jschweers on 12/28/20.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryDictionary {

    private Hashtable<String, Hashtable<String, String>> data = new Hashtable<>();

    @JsonGetter(value="data")
    public Hashtable<String, Hashtable<String, String>> getData() {
        return data;
    }
    @JsonSetter(value="data")
    public void setData(Hashtable<String, Hashtable<String, String>> data) {
        this.data = data;
    }
}
