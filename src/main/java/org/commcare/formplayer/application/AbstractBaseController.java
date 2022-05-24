package org.commcare.formplayer.application;

import datadog.trace.api.Trace;
import lombok.extern.apachecommons.CommonsLog;
import org.commcare.formplayer.beans.InstallRequestBean;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.services.*;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.util.NotificationLogger;
import org.commcare.formplayer.util.serializer.SessionSerializer;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;

/**
 * Base Controller class containing autowired beans used in both MenuController and FormController
 */
@CommonsLog
public abstract class AbstractBaseController {

    @Autowired
    protected FormSessionService formSessionService;

    @Autowired
    protected MenuSessionService menuSessionService;

    @Autowired
    protected InstallService installService;

    @Autowired
    protected RestoreFactory restoreFactory;

    @Autowired
    protected NewFormResponseFactory newFormResponseFactory;

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    protected FormSendCalloutHandler formSendCalloutHandler;

    @Autowired
    protected MenuSessionFactory menuSessionFactory;

    @Autowired
    protected MenuSessionRunnerService runnerService;

    @Autowired
    private NotificationLogger notificationLogger;


    void logNotification(@Nullable NotificationMessage notification, HttpServletRequest req) {
        notificationLogger.logNotification(notification, req);
    }

    @Trace
    protected MenuSession getMenuSessionFromBean(SessionNavigationBean sessionNavigationBean) throws Exception {
        MenuSession menuSession = performInstall(sessionNavigationBean);
        menuSession.setCurrentBrowserLocation(sessionNavigationBean.getGeoLocation());
        return menuSession;
    }

    @Trace
    protected MenuSession performInstall(InstallRequestBean bean) throws Exception {
        if (bean.getAppId() == null || bean.getAppId().isEmpty()) {
            throw new RuntimeException("App_id must not be null.");
        }

        return menuSessionFactory.buildSession(
                bean.getUsername(),
                bean.getDomain(),
                bean.getAppId(),
                bean.getLocale(),
                bean.getOneQuestionPerScreen(),
                bean.getRestoreAs(),
                bean.getPreview()
        );
    }

    @Nullable
    protected CommCareSession getCommCareSession(String menuSessionId) throws Exception {
        if (menuSessionId == null || menuSessionId.trim().equals("")) {
            return null;
        }

        SerializableMenuSession serializableMenuSession = menuSessionService.getSessionById(menuSessionId);
        FormplayerConfigEngine engine = installService.configureApplication(
                serializableMenuSession.getInstallReference(),
                serializableMenuSession.isPreview()).first;
        return SessionSerializer.deserialize(engine.getPlatform(), serializableMenuSession.getCommcareSession());
    }

    protected FormSession getFormSession(SerializableFormSession serializableFormSession) throws Exception {
        CommCareSession commCareSession = getCommCareSession(serializableFormSession.getMenuSessionId());
        return getFormSession(serializableFormSession, commCareSession);
    }

    @NotNull
    protected FormSession getFormSession(SerializableFormSession serializableFormSession,
            @Nullable CommCareSession commCareSession) throws Exception {
        return new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory,
                commCareSession,
                runnerService.getCaseSearchHelper());
    }

}
