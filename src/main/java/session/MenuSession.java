package session;

import auth.BasicAuth;
import auth.HqAuth;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.cli.CommCareSessionException;
import org.commcare.util.cli.EntityScreen;
import org.commcare.util.cli.MenuScreen;
import org.commcare.util.cli.Screen;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import services.InstallService;
import services.RestoreService;

import java.util.HashMap;

/**
 * This (along with FormSession) is a total god object. This manages everything from installation to form entry. This
 * primarily includes module and form navigation, along with case list/details and case selection. When ready,
 * this object will create and hand off flow control to a FormSession object, loading up the proper session data.
 * <p/>
 * A lot of this is copied from the CLI. We need to merge that. Big TODO
 */
@Component
public class MenuSession {
    private FormplayerConfigEngine engine;
    private UserSqlSandbox sandbox;
    private SessionWrapper sessionWrapper;
    private HqAuth auth;
    private String installReference;
    private String username;
    private String domain;
    @Value("${commcarehq.host}")
    private String host;
    private Screen screen;

    Log log = LogFactory.getLog(MenuSession.class);

    public MenuSession(String username, String password, String domain, String appId, String installReference,
                       InstallService installService, RestoreService restoreService) throws Exception {
        //TODO WSP: why host isn't host resolving?
        this.username = username;
        this.domain = domain;

        this.auth = new BasicAuth(username, domain, host, password);

        resolveInstallReference(installReference, appId);

        this.engine = installService.configureApplication(this.installReference, username, "dbs/" + appId);
        this.sandbox = CaseAPIs.restoreIfNotExists(username, restoreService, domain, auth);
        this.sessionWrapper = new SessionWrapper(engine.getPlatform(), sandbox);
        this.screen = getNextScreen();
    }
    private void resolveInstallReference(String installReference, String appId){
        if (installReference == null || installReference.equals("")) {
            if(appId == null || "".equals(appId)){
                throw new RuntimeException("Can't install - either install_reference or app_id must be non-null");
            }
            this.installReference = getReferenceToLatest(appId);
        } else {
            this.installReference = installReference;
        }
    }

    private String getReferenceToLatest(String appId) {
        return "http://localhost:8000/a/" + this.domain +
                "/apps/api/download_ccz/?app_id=" + appId + "#hack=commcare.ccz";
    }

    public boolean handleInput(String input) throws CommCareSessionException {
        log.info("Screen " + screen + " handling input " + input);
        boolean ret = screen.handleInputAndUpdateSession(sessionWrapper, input);
        screen = getNextScreen();
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

    public FormSession getFormEntrySession() throws Exception {
        String formXmlns = sessionWrapper.getForm();
        FormDef formDef = engine.loadFormByXmlns(formXmlns);
        HashMap<String, String> sessionData = getSessionData();
        return new FormSession(sandbox, formDef, "en", username, domain, sessionData);
    }
}
