package session;

import org.commcare.api.json.WalkJson;
import org.commcare.api.xml.XmlUtil;
import org.javarosa.core.model.FormDef;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.parse.XFormParser;
import org.json.JSONArray;

import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

/**
 * Created by willpride on 1/15/16.
 */
public class FormEntrySession {

    private FormDef formDef;
    private FormEntryModel formEntryModel;
    private FormEntryController formEntryController;

    String title;
    String[] langs;
    String uuid;

    public FormEntrySession(String formXml, String initLang) throws IOException {
        System.out.println("Form XML: " + formXml);
        formDef = parseFormDef(formXml);
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        formEntryController.setLanguage(initLang);
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        uuid = UUID.randomUUID().toString();
    }

    private FormDef parseFormDef(String formXml) throws IOException {
        XFormParser mParser = new XFormParser(new StringReader(formXml));
        return mParser.parse();
    }

    public String getFormXml() throws IOException {
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
}
