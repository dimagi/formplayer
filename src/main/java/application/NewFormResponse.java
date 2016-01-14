package application;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by willpride on 1/12/16.
 */
public class NewFormResponse {
    JSONArray tree;
    String title;
    String[] langs;
    String session_id;
    public NewFormResponse(JSONArray tree, String[] langs, String title, String session_id){
        this.tree = tree;
        this.langs = langs;
        this.title = title;
        this.session_id = session_id;
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
