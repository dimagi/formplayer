package beans.debugger;

import beans.CaseBean;
import org.json.JSONArray;
import util.XmlUtils;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

/**
 * Response for the debugger tab
 */
public class DebuggerFormattedQuestionsResponseBean {
    private String xmlns;
    private String appId;
    private String instanceXml;
    private String formattedQuestions;
    private ExternalDataInstanceItem[] instanceList;
    private CaseBean[] cases;
    private AutoCompletableItem[] questionList;

    public DebuggerFormattedQuestionsResponseBean(String appId, String xmlns, String instanceXml,
                                                  String formattedQuestions, JSONArray questionList,
                                                  List<String> functionList,
                                                  Hashtable<String, String> dataInstances,
                                                  CaseBean[] cases) {
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
        initializeInstances(dataInstances);
        this.cases = cases;
        this.questionList = autoCompletable.toArray(new AutoCompletableItem[autoCompletable.size()]);
    }

    private void initializeInstances(Hashtable<String, String> dataInstances) {
        this.instanceList = new ExternalDataInstanceItem[dataInstances.size()];
        Enumeration<String> e = dataInstances.keys();
        int i = 0;
        while(e.hasMoreElements()) {
            String key = e.nextElement();
            instanceList[i] = new ExternalDataInstanceItem(key, dataInstances.get(key));
            i++;
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

    public AutoCompletableItem[] getQuestionList() {
        return questionList;
    }

    public void setQuestionList(AutoCompletableItem[] questionList) {
        this.questionList = questionList;
    }


    public ExternalDataInstanceItem[] getInstanceList() {
        return instanceList;
    }

    public void setInstanceList(ExternalDataInstanceItem[] instanceList) {
        this.instanceList = instanceList;
    }

    public CaseBean[] getCases() {
        return cases;
    }

    public void setCases(CaseBean[] cases) {
        this.cases = cases;
    }
}
