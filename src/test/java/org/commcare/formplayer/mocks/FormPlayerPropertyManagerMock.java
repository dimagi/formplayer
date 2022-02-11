package org.commcare.formplayer.mocks;

import org.commcare.formplayer.util.FormplayerPropertyManager;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 * @author $|-|!˅@M
 */
public class FormPlayerPropertyManagerMock extends FormplayerPropertyManager {

    private boolean fuzzySearch;

    public FormPlayerPropertyManagerMock(IStorageUtilityIndexed properties) {
        super(properties);
    }

    public void enableFuzzySearch(boolean enable) {
        fuzzySearch = enable;
    }

    @Override
    public boolean isFuzzySearchEnabled() {
        return fuzzySearch;
    }
}
