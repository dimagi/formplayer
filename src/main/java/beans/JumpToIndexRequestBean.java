package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Move the current session to the selected index
 */
@ApiModel("Questions for index")
public class JumpToIndexRequestBean extends SessionRequestBean {
    @ApiModelProperty(value = "The FormIndex to be displayed", required = true)
    private int formIndex;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public JumpToIndexRequestBean(){}

    public JumpToIndexRequestBean(int formIndex, String sessionId) {
        this.formIndex = formIndex;
        this.sessionId = sessionId;
    }

    @JsonGetter(value = "ix")
    public int getFormIndex() {
        return formIndex;
    }
    @JsonSetter(value = "ix")
    public void setFormIndex(int formIndex) {
        this.formIndex = formIndex;
    }

    @Override
    public String toString(){
        return "Questions for index nean [formIndex: " + formIndex + ", sessionId: " + sessionId + "]";
    }
}
