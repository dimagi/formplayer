package aspects;

import auth.DjangoAuth;
import auth.HqAuth;
import auth.TokenAuth;
import beans.AuthenticatedRequestBean;
import beans.InstallRequestBean;
import beans.SessionNavigationBean;
import beans.SessionRequestBean;
import hq.models.PostgresUser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import repo.impl.PostgresUserRepo;
import services.FormplayerStorageFactory;
import services.RestoreFactory;
import util.UserUtils;

import java.util.Arrays;

/**
 * Aspect to configure the RestoreFactory
 */
@Aspect
public class AppInstallAspect {

    private final Log log = LogFactory.getLog(AppInstallAspect.class);

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    protected PostgresUserRepo postgresUserRepo;

    @Before(value = "@annotation(annotations.AppInstall)")
    public void configureStorageFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args[0] instanceof InstallRequestBean)) {
            throw new RuntimeException("Could not configure StorageFactory with args " + Arrays.toString(args));
        }
        InstallRequestBean requestBean = (InstallRequestBean) args[0];
        storageFactory.configure(requestBean);
    }
}
