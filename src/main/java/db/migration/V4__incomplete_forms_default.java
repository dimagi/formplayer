package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Add default values to new incomplete forms columns
 * @author wspride
 */
public class V4__incomplete_forms_default extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "ALTER TABLE formplayer_sessions " +
                "ALTER title SET DEFAULT 'CommCare Form'",

                "ALTER TABLE formplayer_sessions " +
                "ALTER dateOpened SET DEFAULT 'Thu Jan 1 00:00:00 UTC 1970'"
        );
    }
}