package session;

import hq.CaseAPIs;
import objects.SerializableFormSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.commcare.api.json.WalkJson;
import org.commcare.api.xml.XmlUtil;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.parse.XFormParser;
import org.json.JSONArray;
import org.springframework.stereotype.Component;
import util.PrototypeUtils;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 *
 * OK this (and MenuSession) is a total god object that basically amanges everything about the state of
 * a form entry session. We turn this into a SerializableFormSession to persist it. Within that we also
 * serialize the formDef to persist the session, in addition to a bunch of other information like the restoreXml.
 * Confusingly we also have a SessionWrapper object within this session which tracks a bunch of other information. There
 * is a lot of unification that needs to happen here.
 *
 * @author willpride
 */
@Component
public class FormEntrySession {

    private FormDef formDef;
    private FormEntryModel formEntryModel;
    private FormEntryController formEntryController;
    private String formXml;
    private String restoreXml;
    private UserSandbox sandbox;
    private int sequenceId;
    private String initLang;
    private Map<String, String> sessionData;
    private FormplayerSessionWrapper sessionWrapper;

    String title;
    String[] langs;
    String uuid;
    String username;

    public FormEntrySession(SerializableFormSession session) throws Exception{
        this.formXml = session.getFormXml();
        this.username = session.getUsername();
        this.sandbox = CaseAPIs.restoreIfNotExists(username, restoreXml);
        this.sessionData = session.getSessionData();
        formDef = new FormDef();
        PrototypeUtils.setupPrototypes();
        deserializeFormDef(session.getFormXml());
        formDef = hq.RestoreUtils.loadInstance(formDef, IOUtils.toInputStream(session.getInstanceXml()));
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        if(session.getInitLang() != null) {
            formEntryController.setLanguage(session.getInitLang());
        }
        this.sequenceId = session.getSequenceId();
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        uuid = UUID.randomUUID().toString();
        this.sequenceId = session.getSequenceId();
        initialize(true, session.getSessionData());
    }

    public FormEntrySession(String formXml, String restoreXml, String initLang, String username,
                Map<String, String> sessionData) throws Exception {
        this.formXml = formXml;
        this.restoreXml = restoreXml;
        this.username = username;
        this.sandbox = CaseAPIs.restoreIfNotExists(username, restoreXml);
        this.sessionData = sessionData;
        formDef = parseFormDef(formXml);
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        formEntryController.setLanguage(initLang);
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        this.initLang = initLang;
        uuid = UUID.randomUUID().toString();
        this.sequenceId = 0;
        initialize(true, sessionData);
    }

    // Entry from menu selection. Assumes user has already been restored.
    public FormEntrySession(UserSandbox sandbox, FormDef formDef, String initLang, String username,
                            Map<String, String> sessionData) throws Exception {
        this.username = username;
        this.sessionData = sessionData;
        this.formDef = formDef;
        this.sandbox = sandbox;
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        formEntryController.setLanguage(initLang);
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        this.initLang = initLang;
        uuid = UUID.randomUUID().toString();
        this.sequenceId = 0;
        initialize(true, sessionData);
    }

    public void initialize(boolean newInstance, Map<String, String> sessionData) throws Exception {
        CommCarePlatform platform = new CommCarePlatform(2, 27);
        this.sessionWrapper = new FormplayerSessionWrapper(platform, this.sandbox, sessionData);
        formDef.initialize(newInstance, sessionWrapper.getIIF());
    }

    private FormDef parseFormDef(String formXml) throws IOException {
        XFormParser mParser = new XFormParser(new StringReader(formXml));
        return mParser.parse();
    }

    public String getInstanceXml() throws IOException {
        return XmlUtil.getPrettyXml(new XFormSerializingVisitor().serializeInstance(formDef.getInstance()));
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
        JSONArray ret = WalkJson.walkToJSON(getFormEntryModel(), getFormEntryController());
        return ret;
    }


    public String getUUID(){
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

    public String getRestoreXml() {
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

    public String getInitLang() {
        return initLang;
    }

    public void setInitLang(String initLang) {
        this.initLang = initLang;
    }

    public UserSandbox getSandbox(){
        return this.sandbox;
    }


    public String submitGetXml() throws IOException {
        formDef.postProcessInstance();
        return getInstanceXml();
    }

    public String serializeFormDef() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream serializedStream = new DataOutputStream(baos);
        formDef.writeExternal(serializedStream);
        String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
        return encoded;
    }

    public void deserializeFormDef(String serializedFormDef) throws IOException, DeserializationException {
        byte [] sessionBytes = Base64.getDecoder().decode(serializedFormDef);
        DataInputStream inputStream =
                new DataInputStream(new ByteArrayInputStream(sessionBytes));
        formDef.readExternal(inputStream, PrototypeManager.getDefault());
    }

    public Map<String, String> getSessionData() {
        return sessionData;
    }

    public SerializableFormSession serialize() throws IOException {
        SerializableFormSession serializableFormSession = new SerializableFormSession();
        serializableFormSession.setInstanceXml(getInstanceXml());
        serializableFormSession.setId(getUUID());
        serializableFormSession.setFormXml(serializeFormDef());
        serializableFormSession.setUsername(username);
        serializableFormSession.setSequenceId(getSequenceId());
        serializableFormSession.setInitLang(getInitLang());
        serializableFormSession.setSessionData(getSessionData());
        return serializableFormSession;
    }
}
