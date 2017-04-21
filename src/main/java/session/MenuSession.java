package session;

import auth.HqAuth;
import engine.FormplayerConfigEngine;
import hq.CaseAPIs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import sandbox.UserSqlSandbox;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.screen.*;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;
import repo.SerializableMenuSession;
import screens.FormplayerQueryScreen;
import screens.FormplayerSyncScreen;
import services.InstallService;
import services.RestoreFactory;
import util.Constants;
import util.SessionUtils;
import util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.UUID;


/**
 * This (along with FormSession) is a total god object. This manages everything from installation to form entry. This
 * primarily includes module and form navigation, along with case list/details and case selection. When ready,
 * this object will create and hand off flow control to a FormSession object, loading up the proper session data.
 *
 * A lot of this is copied from the CLI. We need to merge that. Big TODO
 */
@EnableAutoConfiguration
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
    private String asUser;

    private final Log log = LogFactory.getLog(MenuSession.class);
    private String appId;
    private HqAuth auth;
    private boolean oneQuestionPerScreen;

    public MenuSession(SerializableMenuSession session, InstallService installService,
                       RestoreFactory restoreFactory, HqAuth auth, String host) throws Exception {
        this.username = TableBuilder.scrubName(session.getUsername());
        this.domain = session.getDomain();
        this.asUser = session.getAsUser();
        this.locale = session.getLocale();
        this.uuid = session.getId();
        this.installReference = session.getInstallReference();
        this.auth = auth;

        resolveInstallReference(installReference, appId, host);
        this.engine = installService.configureApplication(this.installReference);

        this.sandbox = CaseAPIs.restoreIfNotExists(restoreFactory, false);

        this.sessionWrapper = new FormplayerSessionWrapper(deserializeSession(engine.getPlatform(), session.getCommcareSession()),
                engine.getPlatform(), sandbox);
        SessionUtils.setLocale(this.locale);
        sessionWrapper.syncState();
        this.screen = getNextScreen();
        this.appId = session.getAppId();
    }

    public MenuSession(String username, String domain, String appId, String installReference, String locale,
                       InstallService installService, RestoreFactory restoreFactory, HqAuth auth, String host,
                       boolean oneQuestionPerScreen, String asUser) throws Exception {
        this.username = TableBuilder.scrubName(username);
        this.domain = domain;
        this.auth = auth;
        this.appId = appId;
        this.asUser = asUser;
        resolveInstallReference(installReference, appId, host);
        this.engine = installService.configureApplication(this.installReference);
        this.sandbox = CaseAPIs.restoreIfNotExists(restoreFactory, false);
        this.sessionWrapper = new FormplayerSessionWrapper(engine.getPlatform(), sandbox);
        this.locale = locale;
        SessionUtils.setLocale(this.locale);
        this.screen = getNextScreen();
        this.uuid = UUID.randomUUID().toString();
        this.oneQuestionPerScreen = oneQuestionPerScreen;
    }
    
    public void updateApp(String updateMode) {
        this.engine.attemptAppUpdate(updateMode);
    }

    private void resolveInstallReference(String installReference, String appId, String host){
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
        builder.addParameter("latest", Constants.CCZ_LATEST_SAVED);
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
        try {
            boolean ret = screen.handleInputAndUpdateSession(sessionWrapper, input);
            screen = getNextScreen();
            log.info("Screen " + screen + " set to " + ret);
            return true;
        } catch(ArrayIndexOutOfBoundsException | NullPointerException e) {
            throw new RuntimeException("Screen " + screen + "  handling input " + input +
                    " threw exception " + e.getMessage() + ". Please try reloading this application" +
                    " and if the problem persists please report a bug.", e);
        }
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
            if (entityScreen.shouldBeSkipped()) {
                return getNextScreen();
            }
            return entityScreen;
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
            computeDatum();
            return getNextScreen();
        } else if(next.equalsIgnoreCase(SessionFrame.STATE_QUERY_REQUEST)) {
            QueryScreen queryScreen = new FormplayerQueryScreen(auth);
            queryScreen.init(sessionWrapper);
            return queryScreen;
        } else if(next.equalsIgnoreCase(SessionFrame.STATE_SYNC_REQUEST)) {
            String username = asUser != null ?
                    StringUtils.getFullUsername(asUser, domain, Constants.COMMCARE_USER_SUFFIX) : null;
            FormplayerSyncScreen syncScreen = new FormplayerSyncScreen(username);
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
            sessionWrapper.setXmlns(FunctionUtils.toString(form.eval(ec)));
            sessionWrapper.setDatum("", "awful");
        } else {
            try {
                sessionWrapper.setDatum(datum.getDataId(), FunctionUtils.toString(form.eval(ec)));
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
        String postUrl = PropertyManager.instance().getSingularProperty("PostURL");
        return new FormSession(sandbox, formDef, username, domain,
                sessionData, postUrl, locale, uuid,
                null, oneQuestionPerScreen,
                asUser, appId, null);
    }

    public void reloadSession(FormSession formSession) throws Exception {
        String formXmlns = formSession.getXmlns();
        FormDef formDef = engine.loadFormByXmlns(formXmlns);
        String postUrl = PropertyManager.instance().getSingularProperty("PostURL");
        formSession.reload(formDef, postUrl);
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

    public String getCommCareVersionString() {
        return sessionWrapper.getIIF().getVersionString();
    }

    public String getAppVersion() {
        return "" + this.engine.getPlatform().getCurrentProfile().getVersion();
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

    public String getAsUser() {
        return asUser;
    }

    public void setAsUser(String asUser) {
        this.asUser = asUser;
    }

    public boolean isOneQuestionPerScreen() {
        return oneQuestionPerScreen;
    }

    public void setOneQuestionPerScreen(boolean oneQuestionPerScreen) {
        this.oneQuestionPerScreen = oneQuestionPerScreen;
    }
}
