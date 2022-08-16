package org.commcare.formplayer.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.commcare.cases.model.Case;
import org.commcare.formplayer.beans.CaseBean;
import org.commcare.formplayer.hq.CaseAPIs;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.StackFrameStep;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.xpath.XPathException;

import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Created by willpride on 4/14/16.
 */
public class SessionUtils {

    private static final Log log = LogFactory.getLog(SessionUtils.class);

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



    public static String resolveInstallReference(String appId, String host, String domain) {
        if (appId == null || "".equals(appId)) {
            throw new RuntimeException("app_id required for install");
        }
        return host + getReferenceToLatest(appId, domain);
    }

    /**
     * Given an app id this returns a URI that will return a CCZ from HQ
     *
     * @param appId An id of the application of the CCZ needed
     * @return An HQ URI to download the CCZ
     */
    public static String getReferenceToLatest(String appId, String domain) {
        URIBuilder builder;
        try {
            builder = new URIBuilder("/a/" + domain + "/apps/api/download_ccz/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to instantiate URIBuilder");
        }
        builder.addParameter("app_id", appId);
        builder.addParameter("latest", Constants.CCZ_LATEST_SAVED);
        return builder.toString();
    }
}
