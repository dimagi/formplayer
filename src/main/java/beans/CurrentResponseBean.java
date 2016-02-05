package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.json.JSONArray;
import session.FormEntrySession;

import java.util.Map;

/**
 * Return the current state of the form entry session (tree, languages, title)
 *
 * Created by willpride on 1/20/16.
 */
public class CurrentResponseBean {
    private JSONArray tree;
    private String title;
    private String[] langs;
    private String sessionId;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public CurrentResponseBean(){}

    public CurrentResponseBean(FormEntrySession session){
        tree = session.getFormTree();
        title = session.getTitle();
        langs = session.getLanguages();
        sessionId = session.getUUID();
    }


    @JsonGetter(value = "session_id")
    public String getSessionId() {
        return sessionId;
    }
    @JsonSetter(value = "session_id")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTree() {
        return tree.toString();
    }

    public void setTree(JSONArray tree) {
        this.tree = tree;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String[] getLangs() {
        return langs;
    }
    public void setLangs(String[] langs) {
        this.langs = langs;
    }

    @Override
    public String toString(){
        return "CurrentResponseBean: [sessionId=" + sessionId + "]";
    }
}
