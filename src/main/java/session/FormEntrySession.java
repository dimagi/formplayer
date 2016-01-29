package session;

import hq.CaseAPIs;
import objects.SerializableSession;
import org.apache.commons.io.IOUtils;
import org.commcare.api.json.WalkJson;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.xml.XmlUtil;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.session.CommCareSession;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.parse.XFormParser;
import org.json.JSONArray;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.UUID;

/**
 * Created by willpride on 1/15/16.
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

    String title;
    String[] langs;
    String uuid;
    String username;

    public FormEntrySession(SerializableSession session) throws Exception{
        this.formXml = session.getFormXml();
        this.restoreXml = session.getRestoreXml();
        formDef = hq.RestoreUtils.loadInstance(IOUtils.toInputStream(formXml), IOUtils.toInputStream(session.getInstanceXml()));
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        if(session.getInitLang() != null) {
            formEntryController.setLanguage(session.getInitLang());
        }
        this.sequenceId = session.getSequenceId();
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        this.username = session.getUsername();
        System.out.println("FormEntrySession RestreXML: " + restoreXml);
        initialize(username, restoreXml);
        uuid = UUID.randomUUID().toString();
    }

    public FormEntrySession(String formXml, String restoreXml, String initLang, String username) throws Exception {
        this.formXml = formXml;
        this.restoreXml = restoreXml;
        formDef = parseFormDef(formXml);
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        formEntryController.setLanguage(initLang);
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        this.initLang = initLang;
        this.username = username;
        System.out.println("FormEntrySession RestreXML: " + restoreXml);
        //initialize(username, restoreXml);
        uuid = UUID.randomUUID().toString();
    }

    public void initialize(String username, String restoreXml) throws Exception {
        this.sandbox = CaseAPIs.restoreIfNotExists(username, restoreXml);
        CommCarePlatform platform = new CommCarePlatform(2, 27);
        CommCareSession session = new CommCareSession(platform);
        formDef.initialize(false, new CommCareInstanceInitializer(session, sandbox, platform));
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

    public String getFormTreeString() {
        String ret = WalkJson.walkToString(getFormEntryModel(), getFormEntryController());
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
}
