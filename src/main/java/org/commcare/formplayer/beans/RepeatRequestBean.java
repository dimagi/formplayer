package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepeatRequestBean extends SessionRequestBean {
    private String repeatIndex;
    private String formIndex;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public RepeatRequestBean(){}

    // in delete commands this will represent the iteration of the repeat to be deleted
    @JsonGetter(value = "ix")
    public String getRepeatIndex() {
        return repeatIndex;
    }
    @JsonSetter(value = "ix")
    public void setRepeatIndex(String repeatIndex) {
        this.repeatIndex = repeatIndex;
    }

    @Override
    public String toString(){
        return "RepeatRequestBean [repeatIndex: " + repeatIndex + ", sessionId: " + sessionId + "]";
    }

    @JsonGetter(value = "form_ix")
    public String getFormIndex() {
        return formIndex;
    }

    @JsonSetter(value = "form_ix")
    public void setFormIndex(String formIndex) {
        this.formIndex = formIndex;
    }
}
