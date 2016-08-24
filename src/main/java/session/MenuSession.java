package session;

import auth.HqAuth;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.*;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.cli.CommCareSessionException;
import org.commcare.util.cli.EntityScreen;
import org.commcare.util.cli.MenuScreen;
import org.commcare.util.cli.QueryScreen;
import org.commcare.util.cli.Screen;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import repo.SerializableMenuSession;
import screens.FormplayerQueryScreen;
import screens.FormplayerSyncScreen;
import services.InstallService;
import services.RestoreService;
import util.SessionUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

import static util.Constants.CCZ_LATEST_SAVED;

/**
 * This (along with FormSession) is a total god object. This manages everything from installation to form entry. This
 * primarily includes module and form navigation, along with case list/details and case selection. When ready,
 * this object will create and hand off flow control to a FormSession object, loading up the proper session data.
 *
 * A lot of this is copied from the CLI. We need to merge that. Big TODO
 */
@Component
public class MenuSession {
    private FormplayerConfigEngine engine;
    private UserSqlSandbox sandbox;
    private SessionWrapper sessionWrapper;
    private String installReference;
    private final String username;
    private final String domain;

    private String locale;
    private Screen screen;
    private String uuid;

    private final Log log = LogFactory.getLog(MenuSession.class);
    private String appId;
    private HqAuth auth;


    private String host;

    public MenuSession(SerializableMenuSession session, InstallService installService,
                       RestoreService restoreService, HqAuth auth) throws Exception {
        this.username = TableBuilder.scrubName(session.getUsername());
        this.domain = session.getDomain();
        this.locale = session.getLocale();
        this.uuid = session.getId();
        this.installReference = session.getInstallReference();

        resolveInstallReference(installReference, appId);
        this.engine = installService.configureApplication(this.installReference, this.username, "dbs/" + appId);
        this.sandbox = CaseAPIs.restoreIfNotExists(this.username, restoreService, domain, auth);
        this.sessionWrapper = new SessionWrapper(deserializeSession(engine.getPlatform(), session.getCommcareSession()),
                engine.getPlatform(), sandbox);
        SessionUtils.setLocale(this.locale);
        sessionWrapper.syncState();
        this.screen = getNextScreen();
        this.auth = auth;
        this.appId = this.engine.getPlatform().getCurrentProfile().getUniqueId();
    }

    public MenuSession(String username, String domain, String appId, String installReference, String locale,
                       InstallService installService, RestoreService restoreService, HqAuth auth) throws Exception {
        this.username = TableBuilder.scrubName(username);
        this.domain = domain;
        resolveInstallReference(installReference, appId);
        this.engine = installService.configureApplication(this.installReference, this.username, "dbs/" + appId);
        this.sandbox = CaseAPIs.restoreIfNotExists(this.username, restoreService, domain, auth);
        this.sessionWrapper = new SessionWrapper(engine.getPlatform(), sandbox);
        this.locale = locale;
        SessionUtils.setLocale(this.locale);
        this.screen = getNextScreen();
        this.uuid = UUID.randomUUID().toString();
        this.auth = auth;
        this.appId = this.engine.getPlatform().getCurrentProfile().getUniqueId();
    }

    private void resolveInstallReference(String installReference, String appId){
        if (installReference == null || installReference.equals("")) {
            if(appId == null || "".equals(appId)){
                throw new RuntimeException("Can't install - either installReference or app_id must be non-null");
            }
            this.installReference = host + getReferenceToLatest(appId);
        } else {
            this.installReference = installReference;
        }
    }

    /**
     * Given an app id this returns a URI that will return a CCZ from HQ
     * @param appId An id of the application of the CCZ needed
     * @return      An HQ URI to download the CCZ
     */
    private String getReferenceToLatest(String appId) {
        URIBuilder builder;
        try {
            builder = new URIBuilder("/a/" + this.domain + "/apps/api/download_ccz/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to instantiate URIBuilder");
        }
        builder.addParameter("app_id", appId);
        builder.addParameter("latest", CCZ_LATEST_SAVED);
        return builder.toString();
    }

    /**
     * @param input the user step input
     * @return Whether or not we were able to evaluate to a new screen.
     */
    public boolean handleInput(String input) throws CommCareSessionException {
        log.info("Screen " + screen + " handling input " + input);
        if(screen == null) {
            return false;
        }
        boolean ret = screen.handleInputAndUpdateSession(sessionWrapper, input);
        screen = getNextScreen();
        log.info("Screen " + screen + " set to " + ret);
        return true;
    }

    public Screen getNextScreen() throws CommCareSessionException {
        String next = sessionWrapper.getNeededData(sessionWrapper.getEvaluationContext());

        if (next == null) {
            //XFORM TIME!
            return null;
        } else if (next.equals(SessionFrame.STATE_COMMAND_ID)) {
            MenuScreen menuScreen = new MenuScreen();
            menuScreen.init(sessionWrapper);
            return menuScreen;
        } else if (next.equals(SessionFrame.STATE_DATUM_VAL)) {
            EntityScreen entityScreen = new EntityScreen();
            entityScreen.init(sessionWrapper);
            return entityScreen;
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
            computeDatum();
            return getNextScreen();
        } else if(next.equalsIgnoreCase(SessionFrame.STATE_QUERY_REQUEST)) {
            QueryScreen queryScreen = new FormplayerQueryScreen(auth);
            queryScreen.init(sessionWrapper);
            return queryScreen;
        } else if(next.equalsIgnoreCase(SessionFrame.STATE_SYNC_REQUEST)) {
            FormplayerSyncScreen syncScreen = new FormplayerSyncScreen();
            syncScreen.init(sessionWrapper);
            return syncScreen;
        }
        throw new RuntimeException("Unexpected Frame Request: " + sessionWrapper.getNeededData());
    }

    private void computeDatum() {
        //compute
        SessionDatum datum = sessionWrapper.getNeededDatum();
        XPathExpression form;
        try {
            form = XPathParseTool.parseXPath(datum.getValue());
        } catch (XPathSyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        EvaluationContext ec = sessionWrapper.getEvaluationContext();
        if (datum instanceof FormIdDatum) {
            sessionWrapper.setXmlns(XPathFuncExpr.toString(form.eval(ec)));
            sessionWrapper.setDatum("", "awful");
        } else {
            try {
                sessionWrapper.setDatum(datum.getDataId(), XPathFuncExpr.toString(form.eval(ec)));
            } catch (XPathException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private HashMap<String, String> getSessionData() {
        OrderedHashtable<String, String> sessionData = sessionWrapper.getData();
        HashMap<String, String> ret = new HashMap<>();
        for (String key : sessionData.keySet()) {
            ret.put(key, sessionData.get(key));
        }
        return ret;
    }

    public FormSession getFormEntrySession() throws Exception {
        String formXmlns = sessionWrapper.getForm();
        FormDef formDef = engine.loadFormByXmlns(formXmlns);
        HashMap<String, String> sessionData = getSessionData();
        String postUrl = new PropertyManager().getSingularProperty("PostURL");
        return new FormSession(sandbox, formDef, username, domain, sessionData, postUrl, locale, uuid);
    }

    private byte[] serializeSession(CommCareSession session){
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        DataOutputStream oos;
        try {
            oos = new DataOutputStream(baos);
            session.serializeSessionState(oos);
        } catch(IOException e){
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    private CommCareSession deserializeSession(CommCarePlatform platform, byte[] bytes) throws DeserializationException, IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        return CommCareSession.restoreSessionFromStream(platform, in);
    }

    public SessionWrapper getSessionWrapper(){
        return sessionWrapper;
    }

    public String getId() {
        return uuid;
    }

    public void setId(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getAppId() {
        return appId;
    }

    public String getInstallReference() {
        return installReference;
    }

    public byte[] getCommcareSession(){
        return serializeSession(sessionWrapper);
    }

    public String getLocale() {
        return locale;
    }

    public void updateScreen() throws CommCareSessionException {
        this.screen = getNextScreen();
    }

    @Value("${commcarehq.host}")
    public void setHost(String host) {
        this.host = host;
    }
}
