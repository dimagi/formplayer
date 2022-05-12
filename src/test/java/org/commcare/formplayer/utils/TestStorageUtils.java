package org.commcare.formplayer.utils;

import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.session.FormplayerInstanceInitializer;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeReference;

import java.util.Hashtable;

/**
 * Created by willpride on 2/21/17.
 */
public class TestStorageUtils {

    /**
     * @return An evaluation context which is capable of evaluating against the connected storage
     * instances: casedb is the only one supported for now
     */
    public static EvaluationContext getEvaluationContextWithoutSession(UserSqlSandbox sandbox) {
        FormplayerInstanceInitializer iif = new FormplayerInstanceInitializer(sandbox);
        return buildEvaluationContext(iif);
    }

    private static EvaluationContext buildEvaluationContext(FormplayerInstanceInitializer iif) {
        ExternalDataInstance edi = new ExternalDataInstance(ExternalDataInstance.JR_CASE_DB_REFERENCE, "casedb");
        DataInstance specializedDataInstance = edi.initialize(iif, "casedb");

        ExternalDataInstance ledgerDataInstanceRaw = new ExternalDataInstance(ExternalDataInstance.JR_LEDGER_DB_REFERENCE,
                "ledgerdb");
        DataInstance ledgerDataInstance = ledgerDataInstanceRaw.initialize(iif, "ledger");

        Hashtable<String, DataInstance> formInstances = new Hashtable<>();
        formInstances.put("casedb", specializedDataInstance);
        formInstances.put("ledger", ledgerDataInstance);

        return new EvaluationContext(new EvaluationContext(null), formInstances,
                TreeReference.rootRef());
    }
}
