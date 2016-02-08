package session;

import auth.BasicAuth;
import auth.HqAuth;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import objects.SerializableMenuSession;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.cli.CommCareSessionException;
import org.commcare.util.cli.EntityScreen;
import org.commcare.util.cli.MenuScreen;
import org.commcare.util.cli.Screen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import services.RestoreService;
import util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

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
    private MenuDisplayable[] choices;

    public MenuSession(String username, String password, String domain,
                       String installReference, String serializedCommCareSession, RestoreService restoreService) throws Exception {
        String domainedUsername = StringUtils.getFullUsername(username, domain, host);
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.installReference = installReference;
        this.auth = new BasicAuth(domainedUsername, password);
        this.engine = configureApplication(installReference);
        sandbox = CaseAPIs.restoreIfNotExists(domainedUsername, restoreService, domain, auth);
        sessionWrapper = new SessionWrapper(engine.getPlatform(), sandbox);
        if(serializedCommCareSession != null){
            restoreSessionFromStream(serializedCommCareSession);
            System.out.println("Loaded Session: " + sessionWrapper.getFrame());
        }
        MenuScreen menuScreen = new MenuScreen();
        menuScreen.init(sessionWrapper);
        choices = menuScreen.getChoices();
    }

    public MenuSession(SerializableMenuSession serializableMenuSession, RestoreService restoreService) throws Exception {
        this(serializableMenuSession.getUsername(), serializableMenuSession.getPassword(), serializableMenuSession.getDomain(),
            serializableMenuSession.getInstallReference(), serializableMenuSession.getSerializedCommCareSession(), restoreService);

    }

    private void restoreSessionFromStream(String serialiedCommCareSession) throws IOException, DeserializationException {
        System.out.println("Restoring session: " + serialiedCommCareSession);
        byte [] sessionBytes = Base64.getDecoder().decode(serialiedCommCareSession);
        SessionFrame restoredFrame = new SessionFrame();
        DataInputStream inputStream =
                new DataInputStream(new ByteArrayInputStream(sessionBytes));
        restoredFrame.readExternal(inputStream, ExtUtil.defaultPrototypes());
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

    public Screen handleInput(String input){
        try {
            int i = Integer.parseInt(input);
            System.out.println("Handle input: " + input + "choices: " + choices[i]);
            String commandId;
            if (choices[i] instanceof Entry) {
                commandId = ((Entry)choices[i]).getCommandId();
            } else {
                commandId = ((Menu)choices[i]).getId();
            }
            sessionWrapper.setCommand(commandId);
            return getNextScreen();
        } catch (NumberFormatException e) {
            //This will result in things just executing again, which is fine.
        } catch (CommCareSessionException e) {
            e.printStackTrace();
        }
        return null;
    }
    private Screen getNextScreen() throws CommCareSessionException {
        String next = sessionWrapper.getNeededData();

        if (next == null) {
            //XFORM TIME!
            return null;
        } else if (next.equals(SessionFrame.STATE_COMMAND_ID)) {
            MenuScreen menuScreen = new MenuScreen();
            menuScreen.init(sessionWrapper);
            return menuScreen;
        } else if (next.equals(SessionFrame.STATE_DATUM_VAL)) {
            return new EntityScreen();
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


    public MenuDisplayable[] getChoices() {
        return choices;
    }

    public void setChoices(MenuDisplayable[] choices) {
        this.choices = choices;
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
}
