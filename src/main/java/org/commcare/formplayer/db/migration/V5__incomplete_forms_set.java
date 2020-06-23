package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Give old incomplete forms a default value
 */
public class V5__incomplete_forms_set extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "UPDATE formplayer_sessions " +
                "SET title='CommCare Form' WHERE title IS NULL",

                "UPDATE formplayer_sessions " +
                "SET dateOpened='Thu Jan 1 00:00:00 UTC 1970' WHERE dateOpened IS NULL"
        );
    }
}
