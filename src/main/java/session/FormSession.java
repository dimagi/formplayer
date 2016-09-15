package session;

import hq.CaseAPIs;
import objects.SerializableFormSession;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.json.JsonActionUtils;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.database.TableBuilder;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.UnregisteredLocaleException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.schema.FormInstanceLoader;
import org.json.JSONArray;
import org.springframework.stereotype.Component;
import util.PrototypeUtils;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 *
 * OK this (and MenuSession) is a total god object that basically mananges everything about the state of
 * a form entry session. We turn this into a SerializableFormSession to persist it. Within that we also
 * serialize the formDef to persist the session, in addition to a bunch of other information like the restoreXml.
 * Confusingly we also have a SessionWrapper object within this session which tracks a bunch of other information. There
 * is a lot of unification that needs to happen here.
 *
 * @author willpride
 */
@Component
public class FormSession {

    Log log = LogFactory.getLog(FormSession.class);

    private FormDef formDef;
    private FormEntryModel formEntryModel;
    private FormEntryController formEntryController;
    private String formXml;
    private String restoreXml;
    private UserSandbox sandbox;
    private int sequenceId;
    private String locale;
    private Map<String, String> sessionData;
    private String postUrl;
    private String title;
    private String[] langs;
    private String uuid;
    private final String username;
    private String domain;
    private String menuSessionId;
    private boolean oneQuestionPerScreen;
    private int currentIndex = -1;


    public FormSession(SerializableFormSession session) throws Exception{
        this.formXml = session.getFormXml();
        this.username = session.getUsername();
        this.restoreXml = session.getRestoreXml();
        this.domain = session.getDomain();
        this.sandbox = CaseAPIs.restoreIfNotExists(username, this.domain, restoreXml);
        this.postUrl = session.getPostUrl();
        this.sessionData = session.getSessionData();
        this.oneQuestionPerScreen = session.getOneQuestionPerScreen();
        formDef = new FormDef();
        PrototypeUtils.setupPrototypes();
        deserializeFormDef(session.getFormXml());
        formDef = FormInstanceLoader.loadInstance(formDef, IOUtils.toInputStream(session.getInstanceXml()));
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        this.sequenceId = session.getSequenceId();
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        uuid = session.getId();
        setLocale(session.getInitLang(), langs);
        this.sequenceId = session.getSequenceId();
        initialize(false, session.getSessionData());
        getFormTree();
        this.menuSessionId = session.getMenuSessionId();
        this.currentIndex = session.getCurrentIndex();
    }

    public FormSession(String formXml, String restoreXml, String locale, String username, String domain,
                       Map<String, String> sessionData, String postUrl,
                       String instanceContent, boolean oneQuestionPerScreen) throws Exception {
        this.formXml = formXml;
        this.restoreXml = restoreXml;
        this.username = TableBuilder.scrubName(username);
        this.sandbox = CaseAPIs.restoreIfNotExists(this.username, domain, restoreXml);
        this.domain = domain;
        this.sessionData = sessionData;
        this.oneQuestionPerScreen = oneQuestionPerScreen;
        if (this.oneQuestionPerScreen) {
            this.currentIndex = 0;
        }
        formDef = parseFormDef(formXml);

        if(instanceContent != null){
            loadInstanceXml(formDef, instanceContent);
            initialize(false, sessionData);
        } else {
            initialize(true, sessionData);
        }

        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        formEntryController.setLanguage(locale);
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        setLocale(locale, langs);
        uuid = UUID.randomUUID().toString();
        this.sequenceId = 0;
        this.postUrl = postUrl;
        getFormTree();
    }

    // Entry from menu selection. Assumes user has already been restored.
    public FormSession(UserSandbox sandbox, FormDef formDef, String username, String domain,
                       Map<String, String> sessionData, String postUrl,
                       String locale, String menuSessionId) throws Exception {
        this.username = TableBuilder.scrubName(username);
        this.formDef = formDef;
        this.sandbox = sandbox;
        this.sessionData = sessionData;
        this.domain = domain;
        this.postUrl = postUrl;
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        langs = formEntryModel.getLanguages();
        setLocale(locale, langs);
        title = formDef.getTitle();
        uuid = UUID.randomUUID().toString();
        this.sequenceId = 0;
        initialize(true, sessionData);
        getFormTree();
        this.postUrl = postUrl;
        this.menuSessionId = menuSessionId;
    }

    private void loadInstanceXml(FormDef formDef, String instanceContent) throws IOException {
        StringReader stringReader = new StringReader(instanceContent);
        XFormParser xFormParser = new XFormParser(stringReader);
        xFormParser.loadXmlInstance(formDef, stringReader);
    }

    private void setLocale(String locale, String[] langs){
        if(locale == null){
            this.locale = langs[0];
        } else{
            this.locale = locale;
        }
        try {
            formEntryController.setLanguage(this.locale);
        } catch (UnregisteredLocaleException e) {
            log.error("Couldn't find locale " + this.locale
                    + " for user " + username);
        }
    }

    private void initialize(boolean newInstance, Map<String, String> sessionData) {
        CommCarePlatform platform = new CommCarePlatform(2, 27);
        FormplayerSessionWrapper sessionWrapper = new FormplayerSessionWrapper(platform, this.sandbox, sessionData);
        formDef.initialize(newInstance, sessionWrapper.getIIF(), locale);
    }

    private FormDef parseFormDef(String formXml) throws IOException {
        XFormParser mParser = new XFormParser(new StringReader(formXml));
        return mParser.parse();
    }

    public String getInstanceXml() throws IOException {
        byte[] bytes = new XFormSerializingVisitor().serializeInstance(formDef.getInstance());
        return new String(bytes, "US-ASCII");
    }

    public FormEntryModel getFormEntryModel(){
        return formEntryModel;
    }

    public FormEntryController getFormEntryController(){
        return formEntryController;
    }

    public String getTitle(){
        return title;
    }

    public String[] getLanguages(){
        return langs;
    }

    public JSONArray getFormTree() {
        if (oneQuestionPerScreen) {
            return JsonActionUtils.getOneQuestionPerScreenJSON(getFormEntryModel(),
                    getFormEntryController(),
                    JsonActionUtils.indexFromString("" + currentIndex, formDef));
        }
        return JsonActionUtils.getFullFormJSON(getFormEntryModel(), getFormEntryController());
    }


    public String getSessionId(){
        return uuid;
    }

    public String getFormXml() {
        return formXml;
    }

    public void setFormXml(String formXml) {
        this.formXml = formXml;
    }

    public String getXmlns(){
        Object metaData = getFormEntryModel().getForm().getMainInstance().getMetaData(FormInstance.META_XMLNS);
        if(metaData == null){
            return null;
        }
        return metaData.toString();
    }

    private String getRestoreXml() {
        return restoreXml;
    }

    public void setRestoreXml(String restoreXml) {
        this.restoreXml = restoreXml;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    private String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public UserSandbox getSandbox(){
        return this.sandbox;
    }


    public String submitGetXml() throws IOException {
        formDef.postProcessInstance();
        return getInstanceXml();
    }

    private Map<String, String> getSessionData(){
        return sessionData;
    }

    private String serializeFormDef() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream serializedStream = new DataOutputStream(baos);
        formDef.writeExternal(serializedStream);
        return Base64.encodeBase64String(baos.toByteArray());
    }

    private void deserializeFormDef(String serializedFormDef) throws IOException, DeserializationException {
        byte [] sessionBytes = Base64.decodeBase64(serializedFormDef);
        DataInputStream inputStream =
                new DataInputStream(new ByteArrayInputStream(sessionBytes));
        formDef.readExternal(inputStream, PrototypeManager.getDefault());
    }


    public SerializableFormSession serialize() throws IOException {
        SerializableFormSession serializableFormSession = new SerializableFormSession();
        serializableFormSession.setInstanceXml(getInstanceXml());
        serializableFormSession.setId(getSessionId());
        serializableFormSession.setFormXml(serializeFormDef());
        serializableFormSession.setUsername(username);
        serializableFormSession.setSessionData(getSessionData());
        serializableFormSession.setSequenceId(getSequenceId());
        serializableFormSession.setInitLang(getLocale());
        serializableFormSession.setSessionData(getSessionData());
        serializableFormSession.setDomain(getDomain());
        serializableFormSession.setRestoreXml(getRestoreXml());
        serializableFormSession.setPostUrl(getPostUrl());
        serializableFormSession.setMenuSessionId(menuSessionId);
        serializableFormSession.setTitle(getTitle());
        serializableFormSession.setDateOpened(new Date().toString());
        serializableFormSession.setOneQuestionPerScreen(oneQuestionPerScreen);
        serializableFormSession.setCurrentIndex(currentIndex);
        return serializableFormSession;
    }

    public String getDomain() {
        return domain;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public String getUsername(){
        return username;
    }

    public String getMenuSessionId() {
        return menuSessionId;
    }

    public void setMenuSessionId(String menuSessionId) {
        this.menuSessionId = menuSessionId;
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
