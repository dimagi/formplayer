package util;

import installers.FormplayerOfflineUserRestoreInstaller;
import installers.FormplayerProfileInstaller;
import installers.FormplayerSuiteInstaller;
import installers.FormplayerXFormInstaller;
import org.commcare.cases.instance.CaseDataInstance;
import org.commcare.core.graph.model.*;
import org.commcare.core.graph.suite.BubbleSeries;
import org.commcare.core.graph.suite.Graph;
import org.commcare.core.graph.suite.XYSeries;
import org.commcare.resources.model.installers.*;
import org.commcare.suite.model.*;
import org.commcare.xml.DummyGraphParser;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.actions.SetValueAction;
import org.javarosa.core.model.data.*;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathConditional;
import org.javarosa.xpath.expr.*;

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
                ComputedDatum.class.getName(),
                EntityDatum.class.getName(),
                FormIdDatum.class.getName(),
                ViewEntry.class.getName(),
                RemoteRequestEntry.class.getName(),
                RemoteQueryDatum.class.getName(),
                XPathPathExpr.class.getName(),
                XPathStringLiteral.class.getName(),
                XPathConditional.class.getName(),
                SubmissionProfile.class.getName(),
                QuestionDef.class.getName(),
                GroupDef.class.getName(),
                FormDef.class.getName(),
                FormInstance.class.getName(),
                ExternalDataInstance.class.getName(),
                XPathCustomRuntimeFunc.class.getName(),
                BooleanData.class.getName(),
                DateData.class.getName(),
                DateTimeData.class.getName(),
                XPathEqExpr.class.getName(),
                XPathArithExpr.class.getName(),
                DecimalData.class.getName(),
                GeoPointData.class.getName(),
                IntegerData.class.getName(),
                LongData.class.getName(),
                PointerAnswerData.class.getName(),
                SelectMultiData.class.getName(),
                SelectOneData.class.getName(),
                StringData.class.getName(),
                TimeData.class.getName(),
                UncastData.class.getName(),
                SetValueAction.class.getName(),
                XPathBoolExpr.class.getName(),
                XPathCmpExpr.class.getName(),
                XPathFilterExpr.class.getName(),
                XPathNumericLiteral.class.getName(),
                XPathNumNegExpr.class.getName(),
                XPathUnionExpr.class.getName(),
                XPathVariableReference.class.getName(),
                FormplayerOfflineUserRestoreInstaller.class.getName(),
                FormplayerProfileInstaller.class.getName(),
                FormplayerSuiteInstaller.class.getName(),
                FormplayerXFormInstaller.class.getName(),
                GraphData.class.getName(),
                SeriesData.class.getName(),
                Graph.class.getName(),
                XYSeries.class.getName(),
                BubbleSeries.class.getName()
        };

        for(Class clazz: FunctionUtils.getXPathFuncListMap().values()){
            PrototypeManager.registerPrototype(clazz.getName());
        }
        PrototypeManager.registerPrototypes(prototypes);
    }
}
