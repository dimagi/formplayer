package session;

import auth.BasicAuth;
import auth.HqAuth;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import objects.SerializableMenuSession;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.session.SessionWrapper;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.util.cli.CommCareSessionException;
import org.commcare.util.cli.MenuScreen;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import services.RestoreService;
import util.StringUtils;

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
    String sessionId;
    private MenuDisplayable[] choices;

    public MenuSession(String username, String password, String domain,
                       String installReference, RestoreService restoreService) throws Exception {
        String domainedUsername = StringUtils.getFullUsername(username, domain, host);
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.installReference = installReference;
        this.auth = new BasicAuth(domainedUsername, password);
        this.engine = configureApplication(installReference);
        sandbox = CaseAPIs.restoreIfNotExists(domainedUsername, restoreService, domain, auth);
        sessionWrapper = new SessionWrapper(engine.getPlatform(), sandbox);
        MenuScreen menuScreen = new MenuScreen();
        menuScreen.init(sessionWrapper);
        choices = menuScreen.getChoices();
    }

    public MenuSession(SerializableMenuSession serializableMenuSession, RestoreService restoreService) throws Exception {
        this(serializableMenuSession.getUsername(), serializableMenuSession.getPassword(), serializableMenuSession.getDomain(),
            serializableMenuSession.getInstallReference(), restoreService);

    }

    public SerializableMenuSession serialize() {
        SerializableMenuSession serializableMenuSession = new SerializableMenuSession();
        serializableMenuSession.setUsername(this.username);
        serializableMenuSession.setPassword(this.password);
        serializableMenuSession.setDomain(this.domain);
        serializableMenuSession.setInstallReference(this.installReference);
        serializableMenuSession.setSessionId(this.sessionId);
        System.out.println("Serialize Session: " + serializableMenuSession);
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

    public MenuDisplayable[] getChoices() {
        return choices;
    }

    public void setChoices(MenuDisplayable[] choices) {
        this.choices = choices;
    }
}
