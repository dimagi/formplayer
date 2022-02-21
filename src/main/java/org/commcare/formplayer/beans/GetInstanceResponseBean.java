package org.commcare.formplayer.beans;

import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.session.FormSession;

import java.io.IOException;

/**
 * Created by willpride on 2/8/16.
 */
public class GetInstanceResponseBean extends BaseResponseBean {

    private String output;
    private String xmlns;

    GetInstanceResponseBean() {
    }

    public GetInstanceResponseBean(FormSession formSession) throws IOException {
        this.output = formSession.getInstanceXml();
        this.xmlns = formSession.getXmlns();
    }

    @Override
    public String toString() {
        return "GetInstanceResponseBean [instanceXml=" + output + "]";
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
