package beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import session.FormSession;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by willpride on 1/12/16.
 */
public class NewFormResponse extends SessionResponseBean {
    private QuestionBean[] tree;
    private String[] langs;
    private String[] breadcrumbs;

    public NewFormResponse(){}

    public NewFormResponse(FormSession fes) throws IOException {
        this.tree = new ObjectMapper().readValue(fes.getFormTree().toString(), QuestionBean[].class);
        this.langs = fes.getLanguages();
        this.title = fes.getTitle();
        this.sessionId = fes.getSessionId();
        this.sequenceId = fes.getSequenceId();
        this.instanceXml = new InstanceXmlBean(fes);
    }

    public QuestionBean[] getTree(){
        return tree;
    }

    public String[] getLangs(){
        return langs;
    }

    public String getSession_id(){return sessionId;}

    public String toString(){
        return "NewFormResponse [sessionId=" + sessionId + ", title=" + title + " tree=" + Arrays.toString(tree) +
                " sequenceId=" + sequenceId + " ]";
    }

    public String[] getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(String[] breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }
}
