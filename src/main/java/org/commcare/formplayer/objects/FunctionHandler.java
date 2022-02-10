package org.commcare.formplayer.objects;

import java.io.Serializable;

/**
 * Created by willpride on 3/4/17.
 */
public class FunctionHandler implements Serializable {
    private String name;
    private String value;

    public FunctionHandler() {
    }

    public FunctionHandler(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
