package beans.debugger;

import beans.SessionResponseBean;
import org.json.JSONArray;
import util.XmlUtils;

/**
 * Response for the debugger tab
 */
public class DebuggerFormattedQuestionsResponseBean {
    private String xmlns;
    private String appId;
    private String instanceXml;
    private String formattedQuestions;
    private QuestionResponseItem[] questionList;

    public DebuggerFormattedQuestionsResponseBean(String appId, String xmlns, String instanceXml, String formattedQuestions, JSONArray questionList) {
        this.xmlns = xmlns;
        this.appId = appId;
        this.instanceXml = XmlUtils.indent(instanceXml);
        this.formattedQuestions = formattedQuestions;
        this.questionList = new QuestionResponseItem[questionList.length()];
        for (int i = 0; i < questionList.length(); i++) {
            this.questionList[i] = new QuestionResponseItem(questionList.getJSONObject(i));
        }
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

    public QuestionResponseItem[] getQuestionList() {
        return questionList;
    }

    public void setQuestionList(QuestionResponseItem[] questionList) {
        this.questionList = questionList;
    }
}
