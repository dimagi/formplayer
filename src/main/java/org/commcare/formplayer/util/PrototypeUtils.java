package org.commcare.formplayer.util;

import org.commcare.cases.instance.CaseDataInstance;
import org.commcare.core.graph.model.GraphData;
import org.commcare.core.graph.model.SeriesData;
import org.commcare.core.graph.suite.BubbleSeries;
import org.commcare.core.graph.suite.Graph;
import org.commcare.core.graph.suite.XYSeries;
import org.commcare.formplayer.installers.FormplayerLocaleInstaller;
import org.commcare.formplayer.installers.FormplayerOfflineUserRestoreInstaller;
import org.commcare.formplayer.installers.FormplayerProfileInstaller;
import org.commcare.formplayer.installers.FormplayerSuiteInstaller;
import org.commcare.formplayer.installers.FormplayerXFormInstaller;
import org.commcare.resources.model.installers.BasicInstaller;
import org.commcare.resources.model.installers.LocaleFileInstaller;
import org.commcare.resources.model.installers.MediaInstaller;
import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.resources.model.installers.XFormInstaller;
import org.commcare.suite.model.ComputedDatum;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.PropertySetter;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.RemoteRequestEntry;
import org.commcare.suite.model.Text;
import org.commcare.suite.model.ViewEntry;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.actions.SendAction;
import org.javarosa.core.model.actions.SetValueAction;
import org.javarosa.core.model.data.BooleanData;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.LongData;
import org.javarosa.core.model.data.PointerAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathConditional;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathArithExpr;
import org.javarosa.xpath.expr.XPathBoolExpr;
import org.javarosa.xpath.expr.XPathCmpExpr;
import org.javarosa.xpath.expr.XPathCustomRuntimeFunc;
import org.javarosa.xpath.expr.XPathDistinctValuesFunc;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathFilterExpr;
import org.javarosa.xpath.expr.XPathNumNegExpr;
import org.javarosa.xpath.expr.XPathNumericLiteral;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.expr.XPathStringLiteral;
import org.javarosa.xpath.expr.XPathUnionExpr;
import org.javarosa.xpath.expr.XPathVariableReference;

/**
 * Created by willpride on 2/8/16.
 */
public class PrototypeUtils {
    public static void setupThreadLocalPrototypes() {
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        String[] prototypes = new String[]{BasicInstaller.class.getName(),
                LocaleFileInstaller.class.getName(),
                SuiteInstaller.class.getName(),
                ProfileInstaller.class.getName(),
                MediaInstaller.class.getName(),
                XFormInstaller.class.getName(),
                Text.class.getName(),
                PropertySetter.class.getName(),
                XPathReference.class.getName(),
                TableLocaleSource.class.getName(),
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
                FormplayerLocaleInstaller.class.getName(),
                GraphData.class.getName(),
                SeriesData.class.getName(),
                Graph.class.getName(),
                XYSeries.class.getName(),
                BubbleSeries.class.getName(),
                SendAction.class.getName(),
                XPathDistinctValuesFunc.class.getName(),
        };

        for (Class clazz : FunctionUtils.getXPathFuncListMap().values()) {
            PrototypeManager.registerPrototype(clazz.getName());
        }
        PrototypeManager.registerPrototypes(prototypes);

        PrototypeManager.useThreadLocalStrategy(true);
    }
}
