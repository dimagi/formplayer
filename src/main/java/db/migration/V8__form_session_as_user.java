package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Give old incomplete forms a default value
 */
public class V8__form_session_as_user extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("ALTER TABLE formplayer_sessions " +
                "ADD asUser VARCHAR");
    }
}
