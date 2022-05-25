package org.commcare.formplayer.beans;

import org.commcare.formplayer.util.XmlUtils;

/**
 * Created by willpride on 1/20/16.
 */
public class InstanceXmlBean {
    private String output;

    // Jackson requires the default constructor be present
    public InstanceXmlBean() {
    }

    public InstanceXmlBean(String xml) {
        output = XmlUtils.indent(xml);
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String toString() {
        return "InstanceXmlBean: [output=" + output + "]";
    }
}
