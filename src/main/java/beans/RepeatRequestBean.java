package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepeatRequestBean extends SessionBean {
    private String formIndex;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public RepeatRequestBean(){}

    @JsonGetter(value = "ix")
    public String getFormIndex() {
        return formIndex;
    }
    @JsonSetter(value = "ix")
    public void setFormIndex(String formIndex) {
        this.formIndex = formIndex;
    }

    @Override
    public String toString(){
        return "RepeatRequestBean [formIndex: " + formIndex + ", sessionId: " + sessionId + "]";
    }
}
