package org.commcare.formplayer.session;

import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.screen.*;
import org.commcare.util.screen.MenuScreen;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.HereFunctionHandlerListener;
import org.javarosa.core.util.MD5;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.commcare.formplayer.repo.SerializableMenuSession;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.formplayer.screens.FormplayerSyncScreen;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.util.*;
import org.commcare.formplayer.util.SessionUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static org.commcare.formplayer.util.SessionUtils.resolveInstallReference;


/**
 * This (along with FormSession) is a total god object. This manages everything from installation to form entry. This
 * primarily includes module and form navigation, along with case list/details and case selection. When ready,
 * this object will create and hand off flow control to a FormSession object, loading up the proper session data.
 *
 * A lot of this is copied from the CLI. We need to merge that. Big TODO
 */
public class MenuSession implements HereFunctionHandlerListener {
    private final SerializableMenuSession session;
    private FormplayerConfigEngine engine;
    private UserSqlSandbox sandbox;
    private SessionWrapper sessionWrapper;

    private final Log log = LogFactory.getLog(MenuSession.class);
    ArrayList<String> breadcrumbs;
    private ArrayList<String> selections = new ArrayList<>();

    private String currentBrowserLocation;
    private boolean hereFunctionEvaluated;

    // Stores the entity screens created to manage state for the lifecycle of this request
    private Map<String, EntityScreen> entityScreenCache = new HashMap<>();

    public MenuSession(SerializableMenuSession session, InstallService installService,
                       RestoreFactory restoreFactory, String host) throws Exception {
        this.session = session;
        this.engine = installService.configureApplication(session.getInstallReference(), session.getPreview()).first;
        this.sandbox = restoreFactory.getSandbox();
        this.sessionWrapper = new FormplayerSessionWrapper(deserializeSession(engine.getPlatform(), session.getCommcareSession()),
                engine.getPlatform(), sandbox);
        SessionUtils.setLocale(session.getLocale());
        sessionWrapper.syncState();
        initializeBreadcrumbs();
    }

    public MenuSession(String username, String domain, String appId, String locale,
                       InstallService installService, RestoreFactory restoreFactory, String host,
                       boolean oneQuestionPerScreen, String asUser, boolean preview) throws Exception {
        String resolvedInstallReference = resolveInstallReference(appId, host, domain);
        this.session = new SerializableMenuSession(
                UUID.randomUUID().toString(),
                TableBuilder.scrubName(username),
                domain,
                appId,
                resolvedInstallReference,
                locale,
                null,
                oneQuestionPerScreen,
                asUser,
                preview
        );
        Pair<FormplayerConfigEngine, Boolean> install = installService.configureApplication(resolvedInstallReference, preview);
        this.engine = install.first;
        if (install.second && !preview && !restoreFactory.getHasRestored()) {
            this.sandbox = restoreFactory.performTimedSync();
        }
        this.sandbox = restoreFactory.getSandbox();
        this.sessionWrapper = new FormplayerSessionWrapper(engine.getPlatform(), sandbox);
        SessionUtils.setLocale(locale);
        initializeBreadcrumbs();
    }

    public void resetSession() {
        this.sessionWrapper = new FormplayerSessionWrapper(engine.getPlatform(), sandbox);
        clearEntityScreenCache();
        initializeBreadcrumbs();
        selections.clear();
    }

    private void initializeBreadcrumbs() {
        this.breadcrumbs = new ArrayList<>();
        this.breadcrumbs.add(SessionUtils.getAppTitle());
    }

    /**
     * Handle a user step, ignoring performance optimizations and not allowing autolaunch actions.
     *
     * @param input The user step input
     */
    public boolean handleInput(String input) throws CommCareSessionException {
        return handleInput(input, true, false, false);
    }

    /**
     * @param input           The user step input
     * @param needsDetail     Whether a full entity screen is required for this request
     *                        or if a list of references is sufficient
     * @param confirmed       Whether the input has been previously validated,
     *                        allowing this step to skip validation
     * @param allowAutoLaunch If this step is allowed to automatically launch an action,
     *                        assuming it has an autolaunch action specified.
     */
    public boolean handleInput(String input, boolean needsDetail, boolean confirmed, boolean allowAutoLaunch) throws CommCareSessionException {
        Screen screen = getNextScreen(needsDetail);
        log.info("Screen " + screen + " handling input " + input);
        if (screen == null) {
            return false;
        }
        try {
            boolean addBreadcrumb = true;
            if (screen instanceof EntityScreen) {
                EntityScreen entityScreen = (EntityScreen)screen;
                boolean autoLaunch = entityScreen.getAutoLaunchAction() != null && allowAutoLaunch;
                addBreadcrumb = !autoLaunch;
                if (input.startsWith("action ") || (autoLaunch) || !confirmed) {
                    screen.init(sessionWrapper);
                    if (screen.shouldBeSkipped()) {
                        return handleInput(input, true, confirmed, allowAutoLaunch);
                    }
                    screen.handleInputAndUpdateSession(sessionWrapper, input, allowAutoLaunch);
                } else {
                    sessionWrapper.setDatum(sessionWrapper.getNeededDatum().getDataId(), input);
                }
            } else {
                boolean ret = screen.handleInputAndUpdateSession(sessionWrapper, input, allowAutoLaunch);
            }
            Screen previousScreen = screen;
            screen = getNextScreen(needsDetail);
            if (addBreadcrumb) {
                addTitle(input, previousScreen);
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            throw new RuntimeException("Screen " + screen + "  handling input " + input +
                    " threw exception " + e.getMessage() + ". Please try reloading this application" +
                    " and if the problem persists please report a bug.", e);
        }
    }

    private void addTitle(String input, Screen previousScreen) {
        if (previousScreen instanceof EntityScreen) {
            try {
                String caseName = SessionUtils.tryLoadCaseName(sandbox.getCaseStorage(), input);
                if (caseName != null) {
                    breadcrumbs.add(caseName);
                    return;
                }
            } catch (NoSuchElementException e) {
                // That's ok, just fallback quietly
            }
        }
        breadcrumbs.add(SessionUtils.getBestTitle(getSessionWrapper()));
    }

    /**
     * Get next screen for current request, based on current state of session,
     * with no performance optimization and autolaunching of actions not allowed.
     */
    public Screen getNextScreen() throws CommCareSessionException {
        return getNextScreen(true);
    }

    /**
     * Get next screen for current request, based on current state of session,
     * with autolaunching of actions not allowed.
     *
     * @param needsDetail Whether a full entity screen is required for this request
     *                    or if a list of references is sufficient
     */
    public Screen getNextScreen(boolean needsDetail) throws CommCareSessionException {
        String next = sessionWrapper.getNeededData(sessionWrapper.getEvaluationContext());
        if (next == null) {
            if (sessionWrapper.isViewCommand(sessionWrapper.getCommand())) {
                sessionWrapper.stepBack();
                return getNextScreen();
            }
            //XFORM TIME!
            return null;
        } else if (next.equals(SessionFrame.STATE_COMMAND_ID)) {
            MenuScreen menuScreen = new MenuScreen();
            menuScreen.init(sessionWrapper);
            return menuScreen;
        } else if (next.equals(SessionFrame.STATE_DATUM_VAL)) {
            EntityScreen entityScreen = getEntityScreenForSession(needsDetail);
            if (entityScreen.shouldBeSkipped()) {
                return getNextScreen();
            }
            return entityScreen;
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
            computeDatum();
            return getNextScreen();
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_QUERY_REQUEST)) {
            QueryScreen queryScreen = new FormplayerQueryScreen();
            queryScreen.init(sessionWrapper);
            return queryScreen;
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_SYNC_REQUEST)) {
            String username = session.getAsUser() != null ?
                    StringUtils.getFullUsername(session.getAsUser(), session.getDomain()) : null;
            FormplayerSyncScreen syncScreen = new FormplayerSyncScreen(username);
            syncScreen.init(sessionWrapper);
            return syncScreen;
        }
        throw new RuntimeException("Unexpected Frame Request: " + sessionWrapper.getNeededData());
    }

    private void clearEntityScreenCache() {
        entityScreenCache.clear();
    }

    private EntityScreen getEntityScreenForSession(boolean needsDetail) throws CommCareSessionException {
        EntityDatum datum = (EntityDatum)sessionWrapper.getNeededDatum();

        //This is only needed because with remote queries there can be nested datums with the same
        //datum ID in the same http request lifecycle.
        String nodesetHash = MD5.toHex(MD5.hash(datum.getNodeset().toString(true).getBytes()));

        String datumKey = datum.getDataId() + ", " + nodesetHash;
        if (!entityScreenCache.containsKey(datumKey)) {
            EntityScreen entityScreen = createFreshEntityScreen(needsDetail);
            entityScreenCache.put(datumKey, entityScreen);
            return entityScreen;
        } else {
            return entityScreenCache.get(datumKey);
        }
    }

    private EntityScreen createFreshEntityScreen(boolean needsDetail) throws CommCareSessionException {
        EntityScreen entityScreen = new EntityScreen(false, needsDetail, sessionWrapper);
        return entityScreen;
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
            sessionWrapper.setDatum(datum.getDataId(), FunctionUtils.toString(form.eval(ec)));
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

    public FormSession getFormEntrySession(FormSendCalloutHandler formSendCalloutHandler,
                                           FormplayerStorageFactory storageFactory) throws Exception {
        String formXmlns = sessionWrapper.getForm();
        FormDef formDef = engine.loadFormByXmlns(formXmlns);
        HashMap<String, String> sessionData = getSessionData();
        String postUrl = sessionWrapper.getPlatform().getPropertyManager().getSingularProperty("PostURL");
        return new FormSession(sandbox, formDef, session.getUsername(), session.getDomain(),
                sessionData, postUrl, session.getLocale(), session.getId(),
                null, session.getOneQuestionPerScreen(),
                session.getAsUser(), session.getAppId(), null, formSendCalloutHandler, storageFactory, false, null);
    }

    private byte[] serializeSession(CommCareSession session) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        DataOutputStream oos;
        try {
            oos = new DataOutputStream(baos);
            session.serializeSessionState(oos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    private CommCareSession deserializeSession(CommCarePlatform platform, byte[] bytes) throws DeserializationException, IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        return CommCareSession.restoreSessionFromStream(platform, in);
    }

    public SessionWrapper getSessionWrapper() {
        return sessionWrapper;
    }

    public String getId() {
        return session.getId();
    }

    public String getAppId() {
        return session.getAppId();
    }

    public String getCommCareVersionString() {
        return sessionWrapper.getIIF().getVersionString();
    }

    public String getAppVersion() {
        return "" + this.engine.getPlatform().getCurrentProfile().getVersion();
    }

    public String[] getBreadcrumbs() {
        String[] ret = new String[breadcrumbs.size()];
        breadcrumbs.toArray(ret);
        return ret;
    }

    public String[] getSelections() {
        String[] ret = new String[selections.size()];
        selections.toArray(ret);
        return ret;
    }

    public void addSelection(String currentStep) {
        selections.add(currentStep);
    }

    public void setCurrentBrowserLocation(String location) {
        this.currentBrowserLocation = location;
    }

    public String getCurrentBrowserLocation() {
        return this.currentBrowserLocation;
    }

    @Override
    public void onEvalLocationChanged() {
    }

    @Override
    public void onHereFunctionEvaluated() {
        this.hereFunctionEvaluated = true;
    }

    public boolean locationRequestNeeded() {
        return this.hereFunctionEvaluated && this.currentBrowserLocation == null;
    }

    public boolean hereFunctionEvaluated() {
        return this.hereFunctionEvaluated;
    }

    public EvaluationContext getEvalContextWithHereFuncHandler() {
        EvaluationContext ec = sessionWrapper.getEvaluationContext();
        ec.addFunctionHandler(new FormplayerHereFunctionHandler(this, currentBrowserLocation));
        return ec;
    }

    public SerializableMenuSession serialize() {
        session.setCommcareSession(serializeSession(sessionWrapper));
        return session;
    }

}
