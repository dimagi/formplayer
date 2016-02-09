package beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import session.FormEntrySession;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by willpride on 1/12/16.
 */
public class NewSessionResponse extends SessionBean{
    // TODO: This should be a QuestionBean array
    QuestionBean[] tree;
    String title;
    String[] langs;

    public NewSessionResponse(){}

    public NewSessionResponse(FormEntrySession fes) throws IOException {
        this.tree = new ObjectMapper().readValue(fes.getFormTree().toString(), QuestionBean[].class);
        this.langs = fes.getLanguages();
        this.title = fes.getTitle();
        this.sessionId = fes.getUUID();
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
}
