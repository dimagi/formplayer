package org.commcare.formplayer.application;

import org.commcare.formplayer.annotations.AppInstall;
import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.beans.InstallRequestBean;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.commcare.formplayer.beans.menus.EntityDetailResponse;
import org.commcare.formplayer.beans.menus.LocationRelevantResponseBean;
import org.commcare.formplayer.beans.menus.UpdateRequestBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.Screen;
import org.javarosa.core.model.instance.TreeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.QueryRequester;
import org.commcare.formplayer.services.SyncRequester;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.util.Constants;

/**
 * Controller (API endpoint) containing all session navigation functionality.
 * This includes module, form, case, and session (incomplete form) selection.
 */
@Api(value = "Menu Controllers", description = "Operations for navigating CommCare Menus and Cases")
@RestController
@EnableAutoConfiguration
public class MenuController extends AbstractBaseController {

    @Autowired
    private QueryRequester queryRequester;

    @Autowired
    private SyncRequester syncRequester;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    private final Log log = LogFactory.getLog(MenuController.class);

    @ApiOperation(value = "Install the application at the given reference")
    @RequestMapping(value = Constants.URL_INSTALL, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @AppInstall
    public BaseResponseBean installRequest(@RequestBody InstallRequestBean installRequestBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken,
                                           HttpServletRequest request) throws Exception {
        BaseResponseBean baseResponseBean = runnerService.getNextMenu(performInstall(installRequestBean));
        logNotification(baseResponseBean.getNotification(), request);
        return baseResponseBean;
    }

    //@ApiOperation(value = "Update the application at the given reference")
    //@RequestMapping(value = Constants.URL_UPDATE, method = RequestMethod.POST)
    //@UserLock
    //@UserRestore
    //@AppInstall
    public NotificationMessage updateRequest(@RequestBody UpdateRequestBean updateRequestBean,
                                             @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken,
                                             HttpServletRequest request) throws Exception {
        NotificationMessage notificationMessage = performUpdate(updateRequestBean);
        logNotification(notificationMessage, request);
        return notificationMessage;
    }

    @RequestMapping(value = Constants.URL_GET_DETAILS, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @AppInstall
    public EntityDetailListResponse getDetails(@RequestBody SessionNavigationBean sessionNavigationBean,
                                               @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken,
                                               HttpServletRequest request) throws Exception {
        MenuSession menuSession = getMenuSessionFromBean(sessionNavigationBean);
        if (sessionNavigationBean.getIsPersistent()) {
            BaseResponseBean baseResponseBean = runnerService.advanceSessionWithSelections(menuSession,
                    sessionNavigationBean.getSelections(),
                    null,
                    sessionNavigationBean.getQueryData(),
                    sessionNavigationBean.getOffset(),
                    sessionNavigationBean.getSearchText(),
                    sessionNavigationBean.getSortIndex(),
                    sessionNavigationBean.isForceManualAction()
            );
            logNotification(baseResponseBean.getNotification(),request);
            // See if we have a persistent case tile to expand
            EntityDetailListResponse detail = runnerService.getInlineDetail(menuSession, storageFactory.getPropertyManager().isFuzzySearchEnabled());
            if (detail == null) {
                throw new RuntimeException("Could not get inline details");
            }
            return setLocationNeeds(detail, menuSession);
        }

        String[] selections = sessionNavigationBean.getSelections();
        String[] commitSelections = new String[selections.length - 1];
        String detailSelection = selections[selections.length - 1];
        System.arraycopy(selections, 0, commitSelections, 0, selections.length - 1);

        BaseResponseBean baseResponseBean = runnerService.advanceSessionWithSelections(
                menuSession,
                commitSelections,
                detailSelection,
                sessionNavigationBean.getQueryData(),
                sessionNavigationBean.getOffset(),
                sessionNavigationBean.getSearchText(),
                sessionNavigationBean.getSortIndex(),
                sessionNavigationBean.isForceManualAction()
        );
        logNotification(baseResponseBean.getNotification(),request);

        Screen currentScreen = menuSession.getNextScreen();

        if (!(currentScreen instanceof EntityScreen)) {
            // See if we have a persistent case tile to expand
            EntityDetailResponse detail = runnerService.getPersistentDetail(menuSession, storageFactory.getPropertyManager().isFuzzySearchEnabled());
            if (detail == null) {
                throw new RuntimeException("Tried to get details while not on a case list.");
            }
            return setLocationNeeds(new EntityDetailListResponse(detail), menuSession);
        }
        EntityScreen entityScreen = (EntityScreen)currentScreen;
        TreeReference reference = entityScreen.resolveTreeReference(detailSelection);

        if (reference == null) {
            throw new RuntimeException("Could not find case with ID " + detailSelection);
        }

        restoreFactory.cacheSessionSelections(selections);
        return setLocationNeeds(
                new EntityDetailListResponse(entityScreen,
                        menuSession.getEvalContextWithHereFuncHandler(),
                        reference,
                        storageFactory.getPropertyManager().isFuzzySearchEnabled()),
                menuSession
        );
    }

    /**
     * Make a a series of menu selections (as above, but can have multiple)
     *
     * @param sessionNavigationBean Give an installation code or path and a set of session selections
     * @param authToken             The Django session id auth token
     * @return A MenuBean or a NewFormResponse
     */
    @RequestMapping(value = {Constants.URL_MENU_NAVIGATION, Constants.URL_INITIAL_MENU_NAVIGATION}, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @AppInstall
    public BaseResponseBean navigateSessionWithAuth(@RequestBody SessionNavigationBean sessionNavigationBean,
                                                    @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken,
                                                    HttpServletRequest request) throws Exception {
        String[] selections = sessionNavigationBean.getSelections();
        MenuSession menuSession;
        menuSession = getMenuSessionFromBean(sessionNavigationBean);
        BaseResponseBean response = runnerService.advanceSessionWithSelections(
                menuSession,
                selections,
                null,
                sessionNavigationBean.getQueryData(),
                sessionNavigationBean.getOffset(),
                sessionNavigationBean.getSearchText(),
                sessionNavigationBean.getSortIndex(),
                sessionNavigationBean.isForceManualAction()
        );
        logNotification(response.getNotification(), request);
        return setLocationNeeds(response, menuSession);
    }

    private static <T extends LocationRelevantResponseBean> T setLocationNeeds(T responseBean, MenuSession menuSession) {
        responseBean.setShouldRequestLocation(menuSession.locationRequestNeeded());
        responseBean.setShouldWatchLocation(menuSession.hereFunctionEvaluated());
        return responseBean;
    }

    private NotificationMessage performUpdate(UpdateRequestBean updateRequestBean) throws Exception {
        MenuSession currentSession = performInstall(updateRequestBean);
        return currentSession.updateApp(updateRequestBean.getUpdateMode());
    }
}
