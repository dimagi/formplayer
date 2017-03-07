package parsers;

import database.models.EntityStorageCache;
import database.models.FormplayerCaseIndexTable;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.xml.CaseXmlParser;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.kxml2.io.KXmlParser;

import java.io.File;
import java.io.IOException;

/**
 * Created by willpride on 3/6/17.
 */
public class FormplayerCaseXmlParser extends CaseXmlParser {
    private File folder;
    private final boolean processAttachments = true;
    private final EntityStorageCache mEntityCache;
    private final FormplayerCaseIndexTable mCaseIndexTable;

    public FormplayerCaseXmlParser(KXmlParser parser, boolean acceptCreateOverwrites,
                                   UserSqlSandbox sandbox) {
        super(parser, acceptCreateOverwrites, sandbox.getCaseStorage());
        mEntityCache = new EntityStorageCache("entitycase", sandbox.getDataSource());
        mCaseIndexTable = new FormplayerCaseIndexTable(sandbox.getDataSource());
    }


    @Override
    public void commit(Case parsed) throws IOException {
        super.commit(parsed);
        mEntityCache.invalidateCache(String.valueOf(parsed.getID()));
        mCaseIndexTable.clearCaseIndices(parsed);
        mCaseIndexTable.indexCase(parsed);
    }
}
