package session;

import auth.BasicAuth;
import auth.HqAuth;
import beans.NewFormSessionResponse;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import objects.SerializableMenuSession;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.cli.CommCareSessionException;
import org.commcare.util.cli.EntityDetailSubscreen;
import org.commcare.util.cli.EntityScreen;
import org.commcare.util.cli.Screen;
import org.commcare.util.cli.MenuScreen;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import repo.SessionRepo;
import services.InstallService;
import services.RestoreService;
import util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This (along with FormSession) is a total god object. This manages everything from installation to form entry. This
 * primarily includes module and form navigation, along with case list/details and case selection. When ready,
 * this object will create and hand off flow control to a FormSession object, loading up the proper session data.
 *
 * A lot of this is copied from the CLI. We need to merge that. Big TODO
 */
@Component
public class MenuSession {
    FormplayerConfigEngine engine;
    UserSqlSandbox sandbox;
    SessionWrapper sessionWrapper;
    HqAuth auth;
    String installReference;
    String username;
    String password;
    String domain;
    String appId;
    @Value("${commcarehq.host}")
    String host;
    private String sessionId;
    private Screen screen;
    private String currentSelection;

    Log log = LogFactory.getLog(MenuSession.class);

    public MenuSession(String username, String password, String domain, String appId,
                       String installReference, String serializedCommCareSession,
                       RestoreService restoreService, String sessionId, String currentSelection, InstallService installService) throws Exception {
        //TODO WSP: why host isn't host resolving?
        String domainedUsername = StringUtils.getFullUsername(username, domain, "commcarehq.org");
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.appId = appId;
        this.installReference = installReference;
        this.auth = new BasicAuth(domainedUsername, password);

        System.out.println("Resolving reference: " + installReference);

        if(installReference == null || installReference.equals("")){
            this.installReference = getReferenceToLatest(appId);
            System.out.println("Resolved reference: " + this.installReference);
        }

        this.engine = installService.configureApplication(this.installReference, username, getDbPath());
        this.currentSelection = currentSelection;

        if(sessionId == null){
            this.sessionId =  UUID.randomUUID().toString();
        } else{
            this.sessionId = sessionId;
        }

        sandbox = CaseAPIs.restoreIfNotExists(domainedUsername, restoreService, domain, auth);
        sessionWrapper = new SessionWrapper(engine.getPlatform(), sandbox);
        if(serializedCommCareSession != null){
            restoreSessionFromStream(serializedCommCareSession);
        }
        screen = getNextScreen();
        if(currentSelection != null){
            if(screen instanceof EntityScreen){
                handleInput(currentSelection);
            }
        }
    }

    private String getReferenceToLatest(String appId) {
        return "http://localhost:8000/a/" + this.domain +
                "/apps/api/download_ccz/?app_id=" + appId + "#hack=commcare.ccz";
    }

    public MenuSession(SerializableMenuSession serializableMenuSession, RestoreService restoreService,
                       InstallService installService) throws Exception {
        this(serializableMenuSession.getUsername(), serializableMenuSession.getPassword(), serializableMenuSession.getDomain(), null,
            serializableMenuSession.getInstallReference(), serializableMenuSession.getSerializedCommCareSession(), restoreService,
                serializableMenuSession.getSessionId(), serializableMenuSession.getCurrentSelection(), installService);

    }

    private void restoreSessionFromStream(String serialiedCommCareSession) throws IOException, DeserializationException {
        byte [] sessionBytes = Base64.decodeBase64(serialiedCommCareSession);
        SessionFrame restoredFrame = new SessionFrame();
        DataInputStream inputStream =
                new DataInputStream(new ByteArrayInputStream(sessionBytes));
        restoredFrame.readExternal(inputStream, new PrototypeFactory());
        this.sessionWrapper.frame = restoredFrame;
        this.sessionWrapper.syncState();
    }

    private String getSerializedCommCareSession() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream serializedStream = new DataOutputStream(baos);
        sessionWrapper.serializeSessionState(serializedStream);
        String encoded = Base64.encodeBase64String(baos.toByteArray());
        return encoded;
    }

    public SerializableMenuSession serialize() throws IOException {
        SerializableMenuSession serializableMenuSession = new SerializableMenuSession();
        serializableMenuSession.setUsername(this.username);
        serializableMenuSession.setPassword(this.password);
        serializableMenuSession.setDomain(this.domain);
        serializableMenuSession.setInstallReference(this.installReference);
        serializableMenuSession.setSessionId(this.sessionId);
        serializableMenuSession.setSerializedCommCareSession(this.getSerializedCommCareSession());
        serializableMenuSession.setCurrentSelection(this.currentSelection);
        return serializableMenuSession;
    }

    public boolean handleInput(String input) throws CommCareSessionException {
        log.info("Screen " + screen + " handling input " + input);
        boolean ret = screen.handleInputAndUpdateSession(sessionWrapper, input);
        log.info("Screen "  + screen + " returning " + ret);
        return ret;
    }
    public Screen getNextScreen() throws CommCareSessionException {
        String next = sessionWrapper.getNeededData();

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

    public HashMap<String, String> getSessionData(){
        OrderedHashtable<String, String> sessionData = sessionWrapper.getData();
        HashMap<String, String> ret = new HashMap<String, String>();
        for(String key: sessionData.keySet()){
            ret.put(key, sessionData.get(key));
        }
        return ret;
    }

    public NewFormSessionResponse startFormEntry(SessionRepo sessionRepo) throws Exception {
        String formXmlns = sessionWrapper.getForm();
        FormDef formDef = engine.loadFormByXmlns(formXmlns);
        HashMap<String, String> sessionData = getSessionData();
        FormSession formEntrySession = new FormSession(sandbox, formDef, "en", username, sessionData);
        sessionRepo.save(formEntrySession.serialize());
        return new NewFormSessionResponse(formEntrySession);
    }

    public String[] getChoices(){
        return screen.getOptions();
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    @Override
    public String toString(){
        return "MenuSession [sessionId=" + sessionId + " choices=" + Arrays.toString(getChoices()) + "]";
    }

    public Screen getCurrentScreen(){
        return screen;
    }

    public Map<Integer, String> getMenuOptions(){
        Map<Integer, String> ret = new HashMap<Integer, String>();
        String[] menuDisplayables = screen.getOptions();
        for(int i = 0; i < menuDisplayables.length; i++){
            ret.put(new Integer(i), menuDisplayables[i]);
        }
        return ret;
    }

    private String getDbPath(){
        if(appId == null){
            return "dbs";
        } else{
            return "dbs/"+appId;
        }
    }

    public void setScreen(Screen screen){
        this.screen = screen;
    }


}
