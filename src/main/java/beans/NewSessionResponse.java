package beans;

import org.json.JSONArray;
import org.json.JSONObject;
import session.FormEntrySession;

import java.util.ArrayList;

/**
 * Created by willpride on 1/12/16.
 */
public class NewSessionResponse {
    // TODO: This should be a QuestionBean array
    JSONArray tree;
    String title;
    String[] langs;
    String session_id;

    public NewSessionResponse(FormEntrySession fes){
        this.tree = fes.getFormTree();
        this.langs = fes.getLanguages();
        this.title = fes.getTitle();
        this.session_id = fes.getUUID();
    }

    public String getTree(){
        return tree.toString();
    }

    public String[] getLangs(){
        return langs;
    }

    public String getTitle(){
        return title;
    }

    public String getSession_id(){return session_id;}
}
