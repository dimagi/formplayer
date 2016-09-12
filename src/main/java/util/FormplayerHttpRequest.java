package util;

import hq.interfaces.CouchUser;
import hq.models.PostgresUser;
import hq.models.SessionToken;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by benrudolph on 9/7/16.
 */
public class FormplayerHttpRequest extends MultipleReadHttpRequest {
    private CouchUser couchUser;
    private PostgresUser postgresUser;
    private String domain;
    private SessionToken token;

    public FormplayerHttpRequest(HttpServletRequest request) {
        super(request);
    }
    public void setCouchUser(CouchUser couchUser) {
        this.couchUser = couchUser;
    }

    public void setPostgresUser(PostgresUser postgresUser) {
        this.postgresUser = postgresUser;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public SessionToken getToken() {
        return token;
    }

    public void setToken(SessionToken token) {
        this.token = token;
    }

    public String getDomain() {
        return domain;
    }

    public PostgresUser getPostgresUser() {
        return postgresUser;
    }

    public CouchUser getCouchUser() {
        return couchUser;
    }
}
