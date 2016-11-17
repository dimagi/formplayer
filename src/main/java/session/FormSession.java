package session;

import beans.CaseBean;
import hq.CaseAPIs;
import objects.SerializableFormSession;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.json.JsonActionUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.database.TableBuilder;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.UnregisteredLocaleException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormController;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.FormInstanceLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;
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
    private String restoreXml;
    private UserSandbox sandbox;
    private int sequenceId;
    private String dateOpened;
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
    private String currentIndex = "-1";
    private boolean isAtLastIndex = false;
    private String asUser;

    private void setupJavaRosaObjects() {
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        initLocale();
    }

    private void setupOneQuestionPerScreen() {
        formEntryController.setOneQuestionPerScreen(oneQuestionPerScreen);
        if (oneQuestionPerScreen) {
            formEntryController.setCurrentIndex(JsonActionUtils.indexFromString(currentIndex, formDef));
        }
    }

    public FormSession(SerializableFormSession session) throws Exception{
        this.username = session.getUsername();
        this.asUser = session.getAsUser();
        this.restoreXml = session.getRestoreXml();
        this.domain = session.getDomain();
        this.sandbox = CaseAPIs.restoreIfNotExists(username, asUser, this.domain, restoreXml);
        this.postUrl = session.getPostUrl();
        this.sessionData = session.getSessionData();
        this.oneQuestionPerScreen = session.getOneQuestionPerScreen();
        this.locale = session.getInitLang();
        this.currentIndex = session.getCurrentIndex();
        this.uuid = session.getId();
        this.sequenceId = session.getSequenceId();
        this.menuSessionId = session.getMenuSessionId();
        this.dateOpened = session.getDateOpened();
        this.formDef = new FormDef();
        deserializeFormDef(session.getFormXml());
        this.formDef = FormInstanceLoader.loadInstance(formDef, IOUtils.toInputStream(session.getInstanceXml()));
        setupJavaRosaObjects();
        initialize(false, session.getSessionData());
        setupOneQuestionPerScreen();
        this.postUrl = session.getPostUrl();
    }

    // New FormSession constructor
    public FormSession(UserSandbox sandbox, FormDef formDef, String username, String domain,
                       Map<String, String> sessionData, String postUrl,
                       String locale, String menuSessionId,
                       String instanceContent, boolean oneQuestionPerScreen, String asUser) throws Exception {
        this.username = TableBuilder.scrubName(username);
        this.formDef = formDef;
        this.sandbox = sandbox;
        this.sessionData = sessionData;
        this.domain = domain;
        this.postUrl = postUrl;
        this.locale = locale;
        this.uuid = UUID.randomUUID().toString();
        this.sequenceId = 0;
        this.postUrl = postUrl;
        this.menuSessionId = menuSessionId;
        this.oneQuestionPerScreen = oneQuestionPerScreen;
        this.asUser = asUser;
        this.currentIndex = "0";
        setupJavaRosaObjects();
        if(instanceContent != null){
            loadInstanceXml(formDef, instanceContent);
            initialize(false, sessionData);
        } else {
            initialize(true, sessionData);
        }
        setupOneQuestionPerScreen();
    }

    private void loadInstanceXml(FormDef formDef, String instanceContent) throws IOException {
        StringReader stringReader = new StringReader(instanceContent);
        XFormParser xFormParser = new XFormParser(stringReader);
        xFormParser.loadXmlInstance(formDef, stringReader);
    }

    private void initLocale(){
        if(locale == null){
            this.locale = this.langs[0];
        }
        try {
            formEntryController.setLanguage(this.locale);
        } catch (UnregisteredLocaleException e) {
            log.error("Couldn't find locale " + this.locale
                    + " for user " + username);
        }
    }

    private void initialize(boolean newInstance, Map<String, String> sessionData) {
        CommCarePlatform platform = new CommCarePlatform(2, 30);
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
                    JsonActionUtils.indexFromString(currentIndex, formDef));
        }
        return JsonActionUtils.getFullFormJSON(getFormEntryModel(), getFormEntryController());
    }


    public String getSessionId(){
        return uuid;
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
        getFormTree();
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
        serializableFormSession.setAsUser(asUser);
        return serializableFormSession;
    }

    public String getDomain() {
        return domain;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public String getUsername(){
        return username;
    }

    public String getMenuSessionId() {
        return menuSessionId;
    }

    public void setCurrentIndex(String index) {
        this.currentIndex = index;
    }

    public String getCurrentIndex() {
        return currentIndex;
    }

    public void setIsAtLastIndex(boolean isAtLastIndex) {
        this.isAtLastIndex = isAtLastIndex;
    }

    public boolean getIsAtLastIndex() {
        return isAtLastIndex;
    }

    public FormDef getFormDef() { return formDef; }

    public void stepToNextIndex() {
        this.formEntryController.jumpToIndex(JsonActionUtils.indexFromString(currentIndex, formDef));
        FormController formController = new FormController(formEntryController, false);
        FormIndex newIndex = formController.getNextFormIndex(formEntryModel.getFormIndex(), true, true);

        // check if this index is the beginning of a group that is not a question list.
        IFormElement element = formEntryController.getModel().getForm().getChild(newIndex);
        while (element instanceof GroupDef && !formEntryController.isFieldListHost(newIndex)) {
            log.info("step thru group");
            newIndex =  formController.getNextFormIndex(newIndex, false, true);
            element = formEntryController.getModel().getForm().getChild(newIndex);
        }

        formEntryController.jumpToIndex(newIndex);

        boolean isEndOfForm = newIndex.isEndOfFormIndex();
        setIsAtLastIndex(isEndOfForm);

        if (!isEndOfForm) {
            setCurrentIndex(newIndex.toString());
        }

    }

    public void stepToPreviousIndex() {
        this.formEntryController.jumpToIndex(JsonActionUtils.indexFromString(currentIndex, formDef));
        FormController formController = new FormController(formEntryController, false);
        FormIndex newIndex = formController.getPreviousFormIndex();

        // check if this index is the beginning of a group that is not a question list.
        IFormElement element = formEntryController.getModel().getForm().getChild(newIndex);
        while (element instanceof GroupDef && !formEntryController.isFieldListHost(newIndex)) {
            newIndex =  formController.getPreviousFormIndex();
            element = formEntryController.getModel().getForm().getChild(newIndex);
        }

        formEntryController.jumpToIndex(newIndex);
        setCurrentIndex(newIndex.toString());
    }

    public JSONObject answerQuestionToJSON(Object answer, String formIndex) {
        JSONObject resp = JsonActionUtils.questionAnswerToJson(formEntryController,
                formEntryModel,
                answer != null ? answer.toString() : null,
                formIndex);
        return resp;
    }

    public JSONObject getNextJson() {
        JSONObject resp = JsonActionUtils.getCurrentJson(formEntryController, formEntryModel, currentIndex);
        resp.put("isAtLastIndex", isAtLastIndex);
        resp.put("currentIndex", currentIndex);
        resp.put("title", title);
        return resp;
    }

    public String getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(String dateOpened) {
        this.dateOpened = dateOpened;
    }

    public String getCaseName() {
        String caseId = this.getSessionData().get("case_id");
        if (caseId == null) {
            return null;
        }
        try {
            CaseBean caseBean = CaseAPIs.getFullCase(caseId, (SqliteIndexedStorageUtility<Case>) this.getSandbox().getCaseStorage());
            return (String) caseBean.getProperties().get("case_name");
        } catch (NoSuchElementException e) {
            // This handles the case where the case is no longer open in the database.
            // The form will crash on open, but I don't know if there's a more elegant but not-opaque way to handle
            return "Case with id " + caseId + "does not exist!";
        }
    }

    public String getAsUser() {
        return asUser;
    }

    public void setAsUser(String asUser) {
        this.asUser = asUser;
    }
}
