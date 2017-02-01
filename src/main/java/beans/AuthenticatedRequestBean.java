package beans;

import auth.DjangoAuth;
import auth.HqAuth;
import auth.TokenAuth;
import hq.models.PostgresUser;
import org.springframework.beans.factory.annotation.Autowired;
import repo.impl.PostgresUserRepo;
import util.Constants;
import util.UserUtils;

/**
 * The AuthenticatedRequestBean should be used for requests that
 * need to be authenticated with HQ. This Bean will ensure the
 * necessary json values are present in the request.
 */
public class AuthenticatedRequestBean {
    @Autowired
    PostgresUserRepo postgresUserRepo;

    protected String domain;
    protected String username;
    protected String restoreAs;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getRestoreAs() {
        return restoreAs;
    }

    public void setRestoreAs(String restoreAs) {
        this.restoreAs = restoreAs;
    }

    public String getUsernameDetail() {
        if (restoreAs != null) {
            return username + "_" + restoreAs;
        }
        return username;
    }

    private boolean isAnonymous() {
        return UserUtils.isAnonymous(domain, username);
    }

    public HqAuth getAuthHeaders(String sessionToken) {
        HqAuth auth;
        if (isAnonymous()) {
            PostgresUser postgresUser = postgresUserRepo.getUserByUsername(username);
            auth = new TokenAuth(postgresUser.getAuthToken());
        } else {
            auth = new DjangoAuth(sessionToken);
        }
        return auth;
    }

    @Override
    public String toString() {
        return "Authenticated request bean wih username=" + username +
                ", domain=" + domain +
                ", restoreAs=" + restoreAs;
    }
}
