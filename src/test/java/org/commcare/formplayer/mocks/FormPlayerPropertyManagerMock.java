package org.commcare.formplayer.mocks;

import static org.mockito.Mockito.when;

import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.util.FormplayerPropertyManager;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.Property;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 * @author $|-|!˅@M
 */
public class FormPlayerPropertyManagerMock extends FormplayerPropertyManager {

    private boolean fuzzySearch;
    private boolean autoAdvanceMenu;

    private boolean indexCaseSearchResults;

    public FormPlayerPropertyManagerMock(IStorageUtilityIndexed properties) {
        super(properties);
    }

    public void enableFuzzySearch(boolean enable) {
        fuzzySearch = enable;
    }

    public void enableAutoAdvanceMenu(boolean enable) {
        autoAdvanceMenu = enable;
    }

    public void enableIndexCaseSearchResults(boolean enable) {
        indexCaseSearchResults = enable;
    }

    @Override
    public boolean isFuzzySearchEnabled() {
        return fuzzySearch;
    }

    @Override
    public boolean isAutoAdvanceMenu() {
        return autoAdvanceMenu;
    }

    @Override
    public boolean isIndexCaseSearchResults() {
        return indexCaseSearchResults;
    }

    // convenience method to set auto advance menu as true
    public static void mockAutoAdvanceMenu(FormplayerStorageFactory storageFactoryMock) {
        SQLiteDB db = storageFactoryMock.getSQLiteDB();
        FormPlayerPropertyManagerMock propertyManagerMock = new FormPlayerPropertyManagerMock(
                new SqlStorage(db, Property.class, PropertyManager.STORAGE_KEY));
        propertyManagerMock.enableAutoAdvanceMenu(true);
        when(storageFactoryMock.getPropertyManager()).thenReturn(propertyManagerMock);
    }
}
