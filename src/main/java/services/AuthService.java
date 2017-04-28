package services;

import auth.HqAuth;
import beans.AuthenticatedRequestBean;

/**
 * Created by willpride on 4/14/17.
 */
public interface AuthService {
    void configureAuth(AuthenticatedRequestBean request);
    void configureAuth(String authToken);
    HqAuth getAuth();
}
