package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import session.FormEntrySession;

import java.io.IOException;

/**
 * Created by willpride on 1/20/16.
 */
public class GetInstanceResponseBean {
    private String output;
    private String xmlns;

    // Jackson requires the default constructor be present
    public GetInstanceResponseBean(){}

    public GetInstanceResponseBean(FormEntrySession session) throws IOException {
        output = session.getInstanceXml();
        xmlns = session.getXmlns();
    }

    public String getXmlns() {
        return xmlns;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String toString(){
        return "GetInstanceResponseBean: [xmlns=" + xmlns + ", output=" + output + "]";
    }
}
