package aspects;

import auth.DjangoAuth;
import auth.HqAuth;
import beans.AuthenticatedRequestBean;
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
import services.CategoryTimingHelper;
import services.RestoreFactory;

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
    protected FormSessionRepo formSessionRepo;

    @Autowired
    CategoryTimingHelper categoryTimingHelper;

    @Value("${touchforms.username:#{null}}")
    private String touchformsUsername;

    @Value("${touchforms.password:#{null}}")
    private String touchformsPassword;

    @Value("${commcarehq.formplayerAuthKey:#{null}}")
    private String authKey;

    @Before(value = "@annotation(annotations.UserRestore)")
    public void configureRestoreFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args[0] instanceof AuthenticatedRequestBean)) {
            throw new RuntimeException(
                    String.format("Could not configure RestoreFactory with invalid request %s", Arrays.toString(args)));
        }
        AuthenticatedRequestBean requestBean = (AuthenticatedRequestBean) args[0];

        HqAuth auth = getHqAuth((String) args[1]);

        configureRestoreFactory(requestBean, auth);

        if (requestBean.isMustRestore()) {
            restoreFactory.performTimedSync();
        }
    }

    private void configureRestoreFactory(AuthenticatedRequestBean requestBean, HqAuth auth) {
        if (requestBean.getRestoreAsCaseId() != null) {
            // SMS user filling out a form as a case
            restoreFactory.configure(requestBean.getDomain(), requestBean.getRestoreAsCaseId(), auth);
            return;
        }
        if (requestBean.getUsername() != null && requestBean.getDomain() != null) {
            // Normal restore path
            restoreFactory.configure(requestBean, auth, requestBean.getUseLiveQuery());
        } else {
            // SMS users don't submit username and domain with each request, so obtain from session
            SerializableFormSession formSession = formSessionRepo.findOneWrapped(requestBean.getSessionId());

            if (formSession.getRestoreAsCaseId() != null) {
                restoreFactory.configure(formSession.getDomain(), formSession.getRestoreAsCaseId(), auth);
            } else {
                restoreFactory.configure(formSession.getUsername(), formSession.getDomain(), formSession.getAsUser(), auth);
            }
        }
    }

    @After(value = "@annotation(annotations.UserRestore)")
    public void closeRestoreFactory(JoinPoint joinPoint) throws Throwable {
        restoreFactory.getSQLiteDB().closeConnection();
    }

    private HqAuth getHqAuth(String sessionToken) {
        if (sessionToken != null) {
            return new DjangoAuth(sessionToken);
        }
        // Null auth expected for SMS requests
        return null;
    }

}
