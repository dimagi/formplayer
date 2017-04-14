package services.impl;

import auth.DjangoAuth;
import auth.HqAuth;
import auth.TokenAuth;
import beans.AuthenticatedRequestBean;
import hq.models.PostgresUser;
import org.springframework.beans.factory.annotation.Autowired;
import repo.impl.PostgresUserRepo;
import services.AuthService;
import util.UserUtils;

/**
 * Created by willpride on 4/14/17.
 */
public class AuthServiceImpl implements AuthService {

    @Autowired
    protected PostgresUserRepo postgresUserRepo;

    HqAuth auth;

    @Override
    public void configureAuth(AuthenticatedRequestBean request, String token) {
        auth = getAuthHeaders(request.getDomain(), request.getUsername(), token);
    }

    @Override
    public HqAuth getAuth() {
        return auth;
    }


    private HqAuth getAuthHeaders(String domain, String username, String token) {
        HqAuth auth;
        if (UserUtils.isAnonymous(domain, username)) {
            PostgresUser postgresUser = postgresUserRepo.getUserByUsername(username);
            auth = new TokenAuth(postgresUser.getAuthToken());
        } else {
            auth = new DjangoAuth(token);
        }
        return auth;
    }
}
