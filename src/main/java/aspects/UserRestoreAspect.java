package aspects;

import auth.DjangoAuth;
import auth.HqAuth;
import auth.TokenAuth;
import beans.AuthenticatedRequestBean;
import hq.models.PostgresUser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import repo.impl.PostgresUserRepo;
import services.RestoreFactory;
import util.UserUtils;

import java.util.Arrays;

/**
 * Aspect to configure the RestoreFactory
 */
@Aspect
public class UserRestoreAspect {

    private final Log log = LogFactory.getLog(UserRestoreAspect.class);

    @Autowired
    protected RestoreFactory restoreFactory;

    @Autowired
    protected PostgresUserRepo postgresUserRepo;

    @Before(value = "@annotation(annotations.UserRestore)")
    public void configureRestoreFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args.length > 1 && args[0] instanceof AuthenticatedRequestBean && args[1] instanceof String)) {
            throw new RuntimeException("Could not configure RestoreFactory with args " + Arrays.toString(args));
        }

        AuthenticatedRequestBean requestBean = (AuthenticatedRequestBean) args[0];
        HqAuth auth = getAuthHeaders(requestBean.getDomain(), requestBean.getUsername(), (String) args[1]);
        restoreFactory.configure((AuthenticatedRequestBean)args[0], auth);
    }

    private HqAuth getAuthHeaders(String domain, String username, String sessionToken) {
        HqAuth auth;
        if (UserUtils.isAnonymous(domain, username)) {
            PostgresUser postgresUser = postgresUserRepo.getUserByUsername(username);
            auth = new TokenAuth(postgresUser.getAuthToken());
        } else {
            auth = new DjangoAuth(sessionToken);
        }
        return auth;
    }

}
