package session;

import auth.BasicAuth;
import auth.HqAuth;
import beans.NewFormSessionResponse;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import javafx.scene.SubScene;
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

/**
 * This (along with FormSession) is a total god object. This manages everything from installation to form entry. This
 * primarily includes module and form navigation, along with case list/details and case selection. When ready,
 * this object will create and hand off flow control to a FormSession object, loading up the proper session data.
 * <p/>
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
    private Screen screen;
    private String currentSelection;

    Log log = LogFactory.getLog(MenuSession.class);

    public MenuSession(String username, String password, String domain, String appId,
                       String installReference, String serializedCommCareSession,
                       String currentSelection, InstallService installService, RestoreService restoreService) throws Exception {
        //TODO WSP: why host isn't host resolving?
        String domainedUsername = StringUtils.getFullUsername(username, domain, "commcarehq.org");
        log.info("Menu session user: " + username + " domain: " + domain + " appId: " + appId);
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.appId = appId;
        this.installReference = installReference;
        this.auth = new BasicAuth(domainedUsername, password);

        if (installReference == null || installReference.equals("")) {
            this.installReference = getReferenceToLatest(appId);
        }

        this.engine = installService.configureApplication(this.installReference, username, "dbs/" + appId);
        this.currentSelection = currentSelection;

        sandbox = CaseAPIs.restoreIfNotExists(username, restoreService, domain, auth);
        sessionWrapper = new SessionWrapper(engine.getPlatform(), sandbox);
        if (serializedCommCareSession != null) {
            restoreSessionFromStream(serializedCommCareSession);
        }
        screen = getNextScreen();
        if (currentSelection != null) {
            if (screen instanceof EntityScreen) {
                handleInput(currentSelection);
            }
        }
    }

    private String getReferenceToLatest(String appId) {
        return "http://localhost:8000/a/" + this.domain +
                "/apps/api/download_ccz/?app_id=" + appId + "#hack=commcare.ccz";
    }

    private void restoreSessionFromStream(String serialiedCommCareSession) throws IOException, DeserializationException {
        byte[] sessionBytes = Base64.decodeBase64(serialiedCommCareSession);
        SessionFrame restoredFrame = new SessionFrame();
        DataInputStream inputStream =
                new DataInputStream(new ByteArrayInputStream(sessionBytes));
        restoredFrame.readExternal(inputStream, new PrototypeFactory());
        this.sessionWrapper.frame = restoredFrame;
        this.sessionWrapper.syncState();
    }

    public boolean handleInput(String input) throws CommCareSessionException {
        log.info("Screen " + screen + " handling input " + input);
        boolean ret = screen.handleInputAndUpdateSession(sessionWrapper, input);
        log.info("Screen " + screen + " returning " + ret);
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

    public HashMap<String, String> getSessionData() {
        OrderedHashtable<String, String> sessionData = sessionWrapper.getData();
        HashMap<String, String> ret = new HashMap<String, String>();
        for (String key : sessionData.keySet()) {
            ret.put(key, sessionData.get(key));
        }
        return ret;
    }

    public NewFormSessionResponse startFormEntry(SessionRepo sessionRepo) throws Exception {
        String formXmlns = sessionWrapper.getForm();
        FormDef formDef = engine.loadFormByXmlns(formXmlns);
        HashMap<String, String> sessionData = getSessionData();
        log.info("Start form entry with username: " + username + " domain " + domain);
        FormSession formEntrySession = new FormSession(sandbox, formDef, "en", username, domain, sessionData);
        sessionRepo.save(formEntrySession.serialize());
        return new NewFormSessionResponse(formEntrySession);
    }

    public String[] getChoices() {
        return screen.getOptions();
    }

    @Override
    public String toString() {
        return "MenuSession [choices=" + Arrays.toString(getChoices()) + "]";
    }

    public Screen getCurrentScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }


}
