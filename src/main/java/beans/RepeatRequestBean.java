package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepeatRequestBean extends SessionBean {
    private String ix;
    private String formIndex;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public RepeatRequestBean(){}

    public String getIx() {
        return ix;
    }
    public void setIx(String ix) {
        this.ix = ix;
    }

    @Override
    public String toString(){
        return "RepeatRequestBean [ix: " + ix + ", sessionId: " + sessionId + "]";
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
