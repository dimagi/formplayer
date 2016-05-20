package beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import session.FormSession;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by willpride on 1/12/16.
 */
public class NewFormSessionResponse extends SessionBean{
    private QuestionBean[] tree;
    private String title;
    private String[] langs;

    public NewFormSessionResponse(){}

    public NewFormSessionResponse(FormSession fes) throws IOException {
        this.tree = new ObjectMapper().readValue(fes.getFormTree().toString(), QuestionBean[].class);
        this.langs = fes.getLanguages();
        this.title = fes.getTitle();
        this.sessionId = fes.getSessionId();
        this.sequenceId = fes.getSequenceId();
    }

    public QuestionBean[] getTree(){
        return tree;
    }

    public String[] getLangs(){
        return langs;
    }

    public String getTitle(){
        return title;
    }

    public String getSession_id(){return sessionId;}

    public String toString(){
        return "NewFormSessionResponse [sessionId=" + sessionId + ", title=" + title + " tree=" + Arrays.toString(tree) +
                " sequenceId=" + sequenceId + " ]";
    }
}
