package org.commcare.formplayer.application;

import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.MenuSessionRunnerService;
import org.commcare.formplayer.services.MenuSessionService;
import org.commcare.formplayer.services.NewFormResponseFactory;
import org.commcare.formplayer.services.RestoreFactory;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.apachecommons.CommonsLog;

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
    protected RestoreFactory restoreFactory;

    @Autowired
    protected NewFormResponseFactory newFormResponseFactory;

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    protected MenuSessionRunnerService runnerService;

}
