package objects;

import java.util.ArrayList;

/**
 * Created by willpride on 2/5/16.
 */
public class SerializableMenuSession {
    private String sessionId;
    private String installReference;
    private String username;
    private String password;
    private String domain;
    private ArrayList<String> actions;

    @Override
    public int hashCode(){
        return sessionId.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof SerializableMenuSession){
            return obj.hashCode() == hashCode();
        }
        return false;
    }

    @Override
    public String toString(){
        return "Session [id=" + sessionId + ", username=" + username + ", actions=" + actions
                + ", installReference=" + installReference + "]";
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getInstallReference() {
        return installReference;
    }

    public void setInstallReference(String installReference) {
        this.installReference = installReference;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public ArrayList<String> getActions() {
        return actions;
    }

    public void setActions(ArrayList<String> actions) {
        this.actions = actions;
    }
}
