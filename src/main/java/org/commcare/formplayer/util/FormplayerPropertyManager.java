package org.commcare.formplayer.util;

import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 * Created by willpride on 3/18/17.
 */
public class FormplayerPropertyManager extends PropertyManager {

    public final static String YES = "yes";
    public final static String NO = "no";
    public final static String NONE = "none";

    public static final String ENABLE_BULK_PERFORMANCE = "cc-enable-bulk-performance";
    public static final String AUTO_PURGE_ENABLED = "cc-auto-purge";

    public static final String POST_FORM_SYNC = "cc-sync-after-form";
    public static final String FUZZY_SEARCH_ENABLED = "cc-fuzzy-search-enabled";

    public static final String SKIP_FIXTURES_AFTER_SUBMIT = "cc-skip-fixtures-after-submit";
    public static final String AUTO_ADVANCE_MENU = "cc-auto-advance-menu";

    /**
     * Constructor for this PropertyManager
     */
    public FormplayerPropertyManager(IStorageUtilityIndexed properties) {
        super(properties);
    }

    private boolean doesPropertyMatch(String key, String defaultValue, String matchingValue) {
        try {
            String property = getSingularProperty(key);
            return property.equals(matchingValue);
        } catch (RuntimeException e) {
            return defaultValue.equals(matchingValue);
        }
    }

    public boolean isBulkPerformanceEnabled() {
        return doesPropertyMatch(ENABLE_BULK_PERFORMANCE, NO, YES);
    }

    public boolean isAutoPurgeEnabled() {
        return doesPropertyMatch(AUTO_PURGE_ENABLED, NO, YES);
    }

    public boolean isSyncAfterFormEnabled() {
        return doesPropertyMatch(POST_FORM_SYNC, NO, YES);
    }

    public boolean isFuzzySearchEnabled() {
        return doesPropertyMatch(FUZZY_SEARCH_ENABLED, NO, YES);
    }

    public boolean skipFixturesAfterSubmit() {
        return doesPropertyMatch(SKIP_FIXTURES_AFTER_SUBMIT, NO, YES);
    }

    public boolean isAutoAdvanceMenu() {
        return doesPropertyMatch(AUTO_ADVANCE_MENU, NO, YES);
    }
}
