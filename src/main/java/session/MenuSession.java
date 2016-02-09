package session;

import auth.BasicAuth;
import auth.HqAuth;
import beans.NewSessionResponse;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import objects.SerializableMenuSession;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.cli.*;
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
import services.RestoreService;
import util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by willpride on 2/5/16.
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
    @Value("${commcarehq.host}")
    String host;
    private String sessionId;
    private Screen screen;
    private String currentSelection;

    public MenuSession(String username, String password, String domain,
                       String installReference, String serializedCommCareSession,
                       RestoreService restoreService, String sessionId, String currentSelection) throws Exception {
        String domainedUsername = StringUtils.getFullUsername(username, domain, host);
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.installReference = installReference;
        this.auth = new BasicAuth(domainedUsername, password);
        this.engine = configureApplication(installReference);
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
            System.out.println("Loaded Session: " + sessionWrapper.getFrame());
        }
        screen = getNextScreen();
        System.out.println("NExt Screen: " +screen);
        System.out.println("Current selection: " + currentSelection);
        if(currentSelection != null){
            if(screen instanceof EntityScreen){
                handleInput(currentSelection);
            }
        }
    }

    public MenuSession(SerializableMenuSession serializableMenuSession, RestoreService restoreService) throws Exception {
        this(serializableMenuSession.getUsername(), serializableMenuSession.getPassword(), serializableMenuSession.getDomain(),
            serializableMenuSession.getInstallReference(), serializableMenuSession.getSerializedCommCareSession(), restoreService,
                serializableMenuSession.getSessionId(), serializableMenuSession.getCurrentSelection());

    }

    private void restoreSessionFromStream(String serialiedCommCareSession) throws IOException, DeserializationException {
        System.out.println("Restoring session: " + serialiedCommCareSession);
        byte [] sessionBytes = Base64.getDecoder().decode(serialiedCommCareSession);
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
        String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
        System.out.println("Storing session: " + encoded);
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

    public FormplayerConfigEngine configureApplication(String installReference){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FormplayerConfigEngine engine = new FormplayerConfigEngine(baos, username);
        if(installReference.endsWith(".ccz")){
            engine.initFromArchive(installReference);
        } else{
            throw new RuntimeException("Can't instantiate with reference: " + installReference);
        }
        engine.initEnvironment();
        return engine;
    }

    public boolean handleInput(String input) throws CommCareSessionException {
        boolean redrawing = screen.handleInputAndUpdateSession(sessionWrapper, input);
        if(screen instanceof EntityScreen){
            EntityScreen entityScreen = (EntityScreen) screen;
            if(entityScreen.getCurrentScreen() instanceof EntityDetailSubscreen){
                EntityDetailSubscreen entityDetailSubscreen = (EntityDetailSubscreen) entityScreen.getCurrentScreen();
                int currentIndex = entityDetailSubscreen.getCurrentIndex();
                currentSelection = input;
            }
        }
        return redrawing;
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
            //TODO: What.
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        EvaluationContext ec = sessionWrapper.getEvaluationContext();
        if (datum.getType() == SessionDatum.DATUM_TYPE_FORM) {
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
        HashMap<String, String> ret = new HashMap<>();
        for(String key: sessionData.keySet()){
            ret.put(key, sessionData.get(key));
        }
        return ret;
    }

    public NewSessionResponse startFormEntry(SessionRepo sessionRepo) throws Exception {
        String formXmlns = sessionWrapper.getForm();
        FormDef formDef = engine.loadFormByXmlns(formXmlns);
        HashMap<String, String> sessionData = getSessionData();
        FormEntrySession formEntrySession = new FormEntrySession(sandbox, formDef, "en", username, sessionData);
        sessionRepo.save(formEntrySession.serialize());
        return new NewSessionResponse(formEntrySession);
    }

    public String[] getChoices(){
        return screen.getOptions();
    }

    public CommCareSession getCommCareSession(){
        return sessionWrapper;
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
}
