package parsers;

import database.models.EntityStorageCache;
import database.models.FormplayerCaseIndexTable;
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

    public FormplayerCaseXmlParser(KXmlParser parser, IStorageUtilityIndexed storage,
                                EntityStorageCache entityCache, FormplayerCaseIndexTable indexTable) {
        super(parser, storage);
        mEntityCache = entityCache;
        mCaseIndexTable = indexTable;
    }

    public FormplayerCaseXmlParser(KXmlParser parser, IStorageUtilityIndexed storage) {
        this(parser, storage, new EntityStorageCache("case"), new FormplayerCaseIndexTable());
    }

    public FormplayerCaseXmlParser(KXmlParser parser, boolean acceptCreateOverwrites,
                                IStorageUtilityIndexed<Case> storage) {
        super(parser, acceptCreateOverwrites, storage);
        mEntityCache = new EntityStorageCache("case");
        mCaseIndexTable = new FormplayerCaseIndexTable();
    }


    @Override
    public void commit(Case parsed) throws IOException {
        super.commit(parsed);
        mEntityCache.invalidateCache(String.valueOf(parsed.getID()));
        mCaseIndexTable.clearCaseIndices(parsed);
        mCaseIndexTable.indexCase(parsed);
    }
}
