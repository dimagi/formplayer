package beans.debugger;

import beans.SessionResponseBean;
import util.XmlUtils;

/**
 * Created by benrudolph on 11/3/16.
 */
public class DebuggerFormattedQuestionsResponseBean {
    private String xmlns;
    private String appId;
    private String instanceXml;
    private String formattedQuestions;

    public DebuggerFormattedQuestionsResponseBean(String appId, String xmlns, String instanceXml, String formattedQuestions) {
        this.xmlns = xmlns;
        this.appId = appId;
        this.instanceXml = XmlUtils.indent(instanceXml);
        this.formattedQuestions = formattedQuestions;
    }

    public String getFormattedQuestions() {
        return formattedQuestions;
    }

    public void setFormattedQuestions(String formattedQuestions) {
        this.formattedQuestions = formattedQuestions;
    }

    public String getXmlns() {
        return xmlns;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }

    public String getInstanceXml() {
        return instanceXml;
    }

    public void setInstanceXml(String instanceXml) {
        this.instanceXml = instanceXml;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
