package org.commcare.formplayer.aspects;

import org.commcare.formplayer.beans.SessionRequestBean;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.javarosa.core.model.FormDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.commcare.formplayer.services.FormplayerStorageFactory;

/**
 * Aspect to configure the FormplayerStorageManager from a restored FormSession
 */
@Aspect
@Order(7)
public class ConfigureStorageFromSessionAspect {

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Before(value = "@annotation(org.commcare.formplayer.annotations.ConfigureStorageFromSession)")
    public void configureStorage(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args[0] instanceof SessionRequestBean)) {
            throw new RuntimeException("Could not configure StorageFactory, request not a SessionRequestBean "
                    + args[0]);
        }
        final SessionRequestBean requestBean = (SessionRequestBean) args[0];
        storageFactory.configure(requestBean.getSessionId());
        storageFactory.getStorageManager().registerStorage(FormDef.STORAGE_KEY, FormDef.class);
    }
}
