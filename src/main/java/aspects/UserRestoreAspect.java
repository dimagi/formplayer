package aspects;

import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import auth.TokenAuth;
import beans.AuthenticatedRequestBean;
import hq.models.PostgresUser;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import repo.FormSessionRepo;
import repo.impl.PostgresUserRepo;
import services.CategoryTimingHelper;
import services.RestoreFactory;
import util.UserUtils;

import java.util.Arrays;

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

    @Value("${touchforms.username}")
    private String touchformsUsername;

    @Value("${touchforms.password}")
    private String touchformsPassword;

    @Value("${commcarehq.formplayerAuthKey}")
    private String authKey;

    @Before(value = "@annotation(annotations.UserRestore)")
    public void configureRestoreFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args.length > 1 && args[0] instanceof AuthenticatedRequestBean && args[1] instanceof String)) {
            throw new RuntimeException("Could not configure RestoreFactory with args " + Arrays.toString(args));
        }
        AuthenticatedRequestBean requestBean = (AuthenticatedRequestBean) args[0];
        HqAuth auth = getAuthHeaders(requestBean.getDomain(), requestBean.getUsername(), (String) args[1]);
        if (requestBean.getCaseId() != null) {
            restoreFactory.configure(requestBean.getDomain(), requestBean.getCaseId(), auth);
        } else {
            AuthenticatedRequestBean request = (AuthenticatedRequestBean) args[0];
            if (request.getSessionId() != null) {
                SerializableFormSession formSession = formSessionRepo.findOneWrapped(request.getSessionId());
                restoreFactory.configure(formSession.getAsUser(), formSession.getDomain(), formSession.getAsUser(), auth);
            } else {
                restoreFactory.configure((AuthenticatedRequestBean) args[0], auth, requestBean.getUseLiveQuery());
            }
        }
        if (requestBean.isMustRestore()) {
            restoreFactory.performTimedSync();
        }
    }

    @After(value = "@annotation(annotations.UserRestore)")
    public void closeRestoreFactory(JoinPoint joinPoint) throws Throwable {
        restoreFactory.getSQLiteDB().closeConnection();
    }

    private HqAuth getAuthHeaders(String domain, String username, String sessionToken) {
        HqAuth auth;
        if (sessionToken != null && sessionToken.equals(authKey)) {
            auth = new BasicAuth(touchformsUsername, touchformsPassword);
        } else if (UserUtils.isAnonymous(domain, username)) {
            PostgresUser postgresUser = postgresUserRepo.getUserByUsername(username);
            auth = new TokenAuth(postgresUser.getAuthToken());
        } else {
            auth = new DjangoAuth(sessionToken);
        }
        return auth;
    }

}
