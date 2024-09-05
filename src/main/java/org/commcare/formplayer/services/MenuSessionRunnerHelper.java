package org.commcare.formplayer.services;

import org.commcare.formplayer.screens.FormplayerSyncScreen;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.exceptions.SyncRestoreException;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.util.screen.ScreenUtils;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MenuSessionRunnerHelper {

    @Autowired
    private WebClient webClient;

    @Autowired
    private FormplayerDatadog datadog;

    @Autowired
    private RestoreFactory restoreFactory;

    /**
     * Execute the post request associated with the sync screen and perform a sync if necessary.
     */
    public void doPostAndSync(MenuSession menuSession, FormplayerSyncScreen screen) throws SyncRestoreException {
        boolean shouldSync = false;
        try {
            if (screen.getSessionSuccessStatus() == null) {
                shouldSync = webClient.caseClaimPost(screen.getUrl(), screen.getQueryParams());
                screen.updateSessionOnSuccess();
            }
        } catch (RestClientResponseException e) {
            throw new SyncRestoreException(
                    String.format("Case claim failed. Message: %s", e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new SyncRestoreException("Unknown error performing case claim", e);
        }
        if (shouldSync) {
            String moduleName = ScreenUtils.getBestTitle(menuSession.getSessionWrapper());
            Map<String, String> extraTags = new HashMap<>();
            Map<String, String> requestScopedTagNameAndValueMap = datadog.getRequestScopedTagNameAndValue();

            requestScopedTagNameAndValueMap.computeIfPresent(Constants.REQUEST_INCLUDES_AUTOSELECT_TAG, extraTags::put);
            extraTags.put(Constants.MODULE_NAME_TAG, moduleName);
            restoreFactory.performTimedSync(false, true, false, extraTags);
            menuSession.getSessionWrapper().clearVolatiles();
        }
    }

}
