package beans;

import session.FormSession;
import util.XmlUtils;

import java.io.IOException;

/**
 * Created by willpride on 1/20/16.
 */
public class InstanceXmlBean {
    private String output;

    // Jackson requires the default constructor be present
    public InstanceXmlBean(){}

    public InstanceXmlBean(FormSession session) throws IOException {
        output = XmlUtils.indent(session.getInstanceXml());
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String toString(){
        return "InstanceXmlBean: [output=" + output + "]";
    }
}
