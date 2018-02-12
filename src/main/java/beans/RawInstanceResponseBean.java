package beans;

import beans.menus.BaseResponseBean;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import session.FormSession;

import java.io.IOException;

/**
 * Created by willpride on 2/8/16.
 */
@ApiModel("Session Response")
public class RawInstanceResponseBean extends BaseResponseBean {

    private String output;
    private String xmlns;

    RawInstanceResponseBean() {}

    public RawInstanceResponseBean(FormSession formSession) throws IOException {
        this.output = formSession.getInstanceXml();
        this.xmlns = formSession.getXmlns();
    }

    @Override
    public String toString() {
        return "RawInstanceResponseBean [instanceXml=" + output + "]";
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getXmlns() {
        return xmlns;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }
}