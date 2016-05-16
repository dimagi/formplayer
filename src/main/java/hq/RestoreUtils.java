package hq;

import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.modern.parse.ParseUtilsHelper;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by willpride on 1/12/16.
 */
public class RestoreUtils {

    public static UserSqlSandbox restoreUser(String username, String path, String restorePayload) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        UserSqlSandbox mSandbox = SqlSandboxUtils.getStaticStorage(username, path);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtilsHelper.parseXMLIntoSandbox(restorePayload, mSandbox);
        return mSandbox;
    }

    /**
     * Build a form definition and load a particular form instance into it.
     * The FormDef object returned isn't initialized, and hence will not have
     * 'instance(...)' data set.
     *
     * @param formDef     the form definition
     * @param instanceInput XML stream of an instance of the form
     * @return The form definition with the given instance loaded. Returns null
     * if the instance doesn't match the form provided.
     * @throws IOException thrown when XML input streams aren't successfully
     *                     parsed
     */
    public static FormDef loadInstance(FormDef formDef,
                                       InputStream instanceInput)
            throws Exception {
        FormInstance savedModel;
        FormEntryModel entryModel;

        savedModel = XFormParser.restoreDataModel(instanceInput, null);

        // get the root of the saved and template instances
        TreeElement savedRoot = savedModel.getRoot();
        TreeElement templateRoot =
                formDef.getInstance().getRoot().deepCopy(true);

        entryModel = new FormEntryModel(formDef);

        // weak check for matching forms
        if (!savedRoot.getName().equals(templateRoot.getName()) ||
                savedRoot.getMult() != 0) {
            throw new Exception("Instance and template form definition don't match");
        } else {
            // populate the data model
            TreeReference tr = TreeReference.rootRef();
            tr.add(templateRoot.getName(), TreeReference.INDEX_UNBOUND);
            templateRoot.populate(savedRoot);

            // populated model to current form
            formDef.getInstance().setRoot(templateRoot);

            if (entryModel.getLanguages() != null) {
                formDef.localeChanged(entryModel.getLanguage(),
                        formDef.getLocalizer());
            }
        }

        return formDef;
    }
}
