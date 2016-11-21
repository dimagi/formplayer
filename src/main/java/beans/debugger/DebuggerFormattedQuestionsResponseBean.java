package beans.debugger;

import org.json.JSONArray;
import util.XmlUtils;

import java.util.HashSet;
import java.util.List;

/**
 * Response for the debugger tab
 */
public class DebuggerFormattedQuestionsResponseBean {
    private String xmlns;
    private String appId;
    private String instanceXml;
    private String formattedQuestions;
    private AutoCompletableItem[] questionList;

    public DebuggerFormattedQuestionsResponseBean(String appId, String xmlns, String instanceXml,
                                                  String formattedQuestions, JSONArray questionList, List<String> functionList) {
        this.xmlns = xmlns;
        this.appId = appId;
        this.instanceXml = XmlUtils.indent(instanceXml);
        this.formattedQuestions = formattedQuestions;
        HashSet<AutoCompletableItem> autoCompletable = new HashSet<>();
        for (int i = 0; i < questionList.length(); i++) {
            autoCompletable.add(new AutoCompletableItem(questionList.getJSONObject(i)));
        }
        for (String function: functionList) {
            autoCompletable.add(new FunctionAutocompletable(function));
        }
        this.questionList = autoCompletable.toArray(new AutoCompletableItem[autoCompletable.size()]);
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

    public AutoCompletableItem[] getQuestionList() {
        return questionList;
    }

    public void setQuestionList(AutoCompletableItem[] questionList) {
        this.questionList = questionList;
    }
}
