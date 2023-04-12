package org.commcare.formplayer.parsers;

import org.commcare.formplayer.database.models.EntityStorageCache;
import org.commcare.formplayer.database.models.FormplayerCaseIndexTable;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.xml.CaseXmlParser;
import org.kxml2.io.KXmlParser;

import java.io.IOException;

/**
 * Created by willpride on 3/6/17.
 */
public class FormplayerCaseXmlParser extends CaseXmlParser {
    private final EntityStorageCache mEntityCache;
    private final FormplayerCaseIndexTable mCaseIndexTable;
    public FormplayerCaseXmlParser(KXmlParser parser, boolean acceptCreateOverwrites,
                                   UserSqlSandbox sandbox) {
        super(parser, acceptCreateOverwrites, sandbox.getCaseStorage());
        mEntityCache = new EntityStorageCache("entitycase", sandbox);
        mCaseIndexTable = new FormplayerCaseIndexTable(sandbox);
    }


    @Override
    public void commit(Case parsed) throws IOException {
        super.commit(parsed);
        mEntityCache.invalidateCache(String.valueOf(parsed.getID()));
        mCaseIndexTable.clearCaseIndices(parsed);
        mCaseIndexTable.indexCase(parsed);
    }
}
