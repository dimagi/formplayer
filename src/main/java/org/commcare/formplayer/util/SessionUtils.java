package org.commcare.formplayer.util;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.beans.CaseBean;
import org.commcare.formplayer.hq.CaseAPIs;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import java.util.NoSuchElementException;

import okhttp3.HttpUrl;

/**
 * Created by willpride on 4/14/16.
 */
public class SessionUtils {

    public static String tryLoadCaseName(SqlStorage<Case> caseStorage, String caseId)
            throws NoSuchElementException {
        if (caseId == null) {
            return null;
        }
        CaseBean caseBean = CaseAPIs.getFullCase(caseId, caseStorage);
        return (String)caseBean.getProperties().get("case_name");
    }

    public static void setLocale(String locale) {
        if (locale == null || "".equals(locale.trim())) {
            return;
        }
        Localizer localizer = Localization.getGlobalLocalizerAdvanced();
        if (localizer.hasLocale(locale)) {
            localizer.setLocale(locale);
        }
    }

    public static String resolveInstallReference(String appId, String host, String domain, String appVersion) {
        if (appId == null || "".equals(appId)) {
            throw new RuntimeException("app_id required for install");
        }
        // Conditional check is for backwards compatability. Once HQ changes are deployed to include
        // app version in URL, this can be removed.
        if (appVersion != null) {
            return getReferenceToVersion(host, appId, domain, appVersion);
        } else {
            return getReferenceToLatest(host, appId, domain);
        }
    }

    /**
     * Given an app id this returns a URI that will return a CCZ from HQ
     *
     * @param appId An id of the application of the CCZ needed
     * @return An HQ URI to download the CCZ
     */
    public static String getReferenceToLatest(String host, String appId, String domain) {
        HttpUrl.Builder builder;
        builder = HttpUrl.parse(host).newBuilder()
                .addPathSegments("a/" + domain + "/apps/api/download_ccz/")
                .addQueryParameter("app_id", appId)
                .addQueryParameter("latest", Constants.CCZ_LATEST_SAVED);
        return builder.toString();
    }

    /**
     * Given a canonical app id this returns a URI that will return the specified build version CCZ from HQ
     *
     * @param appId The canonical id of the application of the CCZ needed
     * @return An HQ URI to download the CCZ
     */
    private static String getReferenceToVersion(String host, String appId, String domain, String appVersion) {
        HttpUrl.Builder builder;
        builder = HttpUrl.parse(host).newBuilder()
                .addPathSegments("a/" + domain + "/apps/api/download_ccz/")
                .addQueryParameter("app_id", appId)
                .addQueryParameter("version", appVersion);
        return builder.toString();
    }
}
