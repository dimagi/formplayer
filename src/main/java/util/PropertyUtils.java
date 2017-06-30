package util;

import org.javarosa.core.services.PropertyManager;

/**
 * Created by willpride on 3/18/17.
 */
public class PropertyUtils {

    public final static String YES = "yes";
    public final static String NO = "no";
    public final static String NONE = "none";

    public static final String ENABLE_BULK_PERFORMANCE = "cc-enable-bulk-performance";
    public static final String AUTO_PURGE_ENABLED = "cc-auto-purge";
    public static final String ENABLE_LIVE_QUERY = "cc-enable-live-query";

    private static boolean doesPropertyMatch(String key, String defaultValue, String matchingValue) {
        try {
            String property = PropertyManager.instance().getSingularProperty(key);
            return property.equals(matchingValue);
        } catch (RuntimeException e) {
            return defaultValue.equals(matchingValue);
        }
    }


    public static boolean isLiveQueryEnabled() {
        return doesPropertyMatch(ENABLE_LIVE_QUERY, NO, YES);
    }

    public static boolean isBulkPerformanceEnabled() {
        return doesPropertyMatch(ENABLE_BULK_PERFORMANCE, NO, YES);
    }

    public static boolean isAutoPurgeEnabled() {
        return doesPropertyMatch(AUTO_PURGE_ENABLED, NO, YES);
    }
}
