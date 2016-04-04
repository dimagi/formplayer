package util;

import org.commcare.cases.instance.CaseDataInstance;
import org.commcare.resources.model.installers.*;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.PropertySetter;
import org.commcare.suite.model.Text;
import org.commcare.xml.DummyGraphParser;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;

/**
 * Created by willpride on 2/8/16.
 */
public class PrototypeUtils {
    public static void setupPrototypes(){
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        String[] prototypes = new String[] {BasicInstaller.class.getName(),
                LocaleFileInstaller.class.getName(),
                SuiteInstaller.class.getName(),
                ProfileInstaller.class.getName(),
                MediaInstaller.class.getName(),
                XFormInstaller.class.getName(),
                Text.class.getName(),
                PropertySetter.class.getName(),
                XPathReference.class.getName(),
                TableLocaleSource.class.getName(),
                DummyGraphParser.DummyGraphDetailTemplate.class.getName(),
                FormEntry.class.getName(),
                CaseDataInstance.class.getName(),
                QuestionDef.class.getName(),
                "org.javarosa.xpath.XPathConditional",
                "org.javarosa.core.model.SubmissionProfile",
                "org.javarosa.core.model.QuestionDef",
                "org.javarosa.core.model.GroupDef",
                "org.javarosa.core.model.instance.FormInstance",
                "org.javarosa.core.model.instance.ExternalDataInstance",
                "org.javarosa.core.model.data.BooleanData",
                "org.javarosa.core.model.data.DateData",
                "org.javarosa.core.model.data.DateTimeData",
                "org.javarosa.core.model.data.DecimalData",
                "org.javarosa.core.model.data.GeoPointData",
                "org.javarosa.core.model.data.IntegerData",
                "org.javarosa.core.model.data.LongData",
                "org.javarosa.core.model.data.PointerAnswerData",
                "org.javarosa.core.model.data.SelectMultiData",
                "org.javarosa.core.model.data.SelectOneData",
                "org.javarosa.core.model.data.StringData",
                "org.javarosa.core.model.data.TimeData",
                "org.javarosa.core.model.data.UncastData",
                "org.javarosa.core.model.actions.SetValueAction",
                "org.javarosa.xpath.expr.XPathArithExpr",
                "org.javarosa.xpath.expr.XPathBoolExpr",
                "org.javarosa.xpath.expr.XPathCmpExpr",
                "org.javarosa.xpath.expr.XPathEqExpr",
                "org.javarosa.xpath.expr.XPathFilterExpr",
                "org.javarosa.xpath.expr.XPathFuncExpr",
                "org.javarosa.xpath.expr.XPathNumericLiteral",
                "org.javarosa.xpath.expr.XPathNumNegExpr",
                "org.javarosa.xpath.expr.XPathPathExpr",
                "org.javarosa.xpath.expr.XPathStringLiteral",
                "org.javarosa.xpath.expr.XPathUnionExpr",
                "org.javarosa.xpath.expr.XPathVariableReference"};
        PrototypeManager.registerPrototypes(prototypes);
    }
}
