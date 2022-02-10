package org.commcare.formplayer.hq;

import org.commcare.formplayer.beans.CaseBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.model.Case;
import org.commcare.formplayer.sandbox.SqlStorage;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    private static final Log log = LogFactory.getLog(CaseAPIs.class);

    public static CaseBean getFullCase(String caseId, SqlStorage<Case> caseStorage) {
        Case cCase = caseStorage.getRecordForValue("case-id", caseId);
        return new CaseBean(cCase);
    }
}
