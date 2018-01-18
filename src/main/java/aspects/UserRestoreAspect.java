package aspects;

import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import auth.TokenAuth;
import beans.AuthenticatedRequestBean;
import hq.CaseAPIs;
import hq.models.PostgresUser;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import repo.FormSessionRepo;
import repo.impl.PostgresUserRepo;
import services.CategoryTimingHelper;
import services.RestoreFactory;
import session.FormSession;
import util.Constants;
import util.Timing;
import util.UserUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * Aspect to configure the RestoreFactory
 */
@Aspect
@Order(5)
public class UserRestoreAspect {

    private final Log log = LogFactory.getLog(UserRestoreAspect.class);

    @Autowired
    protected RestoreFactory restoreFactory;

    @Autowired
    protected PostgresUserRepo postgresUserRepo;

    @Autowired
    protected FormSessionRepo formSessionRepo;

    @Autowired
    CategoryTimingHelper categoryTimingHelper;

    @Before(value = "@annotation(annotations.UserRestore)")
    public void configureRestoreFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args.length > 1 && args[0] instanceof AuthenticatedRequestBean && args[1] instanceof String)) {
            throw new RuntimeException("Could not configure RestoreFactory with args " + Arrays.toString(args));
        }

        AuthenticatedRequestBean requestBean = (AuthenticatedRequestBean) args[0];

        if (requestBean.getSessionId() != null) {
            SerializableFormSession formSession = formSessionRepo.findOne(requestBean.getSessionId());
            String username = formSession.getUsername();
            String domain = formSession.getDomain();
            String asUsername = formSession.getAsUser();
            HqAuth auth = null;
            if ("abcd".equals(asUsername)) {
                auth = new BasicAuth(asUsername, "123");
            } else if ("efgh".equals(asUsername)) {
                auth = new BasicAuth(asUsername, "122");
            } else if ("xyz".equals(asUsername)) {
                auth = new BasicAuth(asUsername, "121");
            }
            restoreFactory.configure(username, domain, asUsername, auth, false);
        }
        else {
            HqAuth auth = getAuthHeaders(requestBean.getDomain(), requestBean.getUsername(), (String) args[1], requestBean.getHqAuth(),
                    requestBean.getRestoreAs());
            restoreFactory.configure((AuthenticatedRequestBean) args[0], auth, requestBean.getUseLiveQuery());
        }

        if (requestBean.isMustRestore()) {
            restoreFactory.performTimedSync();
        }
    }

    @After(value = "@annotation(annotations.UserRestore)")
    public void closeRestoreFactory(JoinPoint joinPoint) throws Throwable {
        restoreFactory.getSQLiteDB().closeConnection();
    }

    private HqAuth getAuthHeaders(String domain, String username, String sessionToken, Map<String, String> hqAuth, String asUsername) {
        HqAuth auth;
        if (hqAuth != null) {
            auth = new BasicAuth(asUsername, hqAuth.get("key"));
        } else if (UserUtils.isAnonymous(domain, username)) {
            PostgresUser postgresUser = postgresUserRepo.getUserByUsername(username);
            auth = new TokenAuth(postgresUser.getAuthToken());
        } else {
            auth = new DjangoAuth(sessionToken);
        }
        return auth;
    }

}
