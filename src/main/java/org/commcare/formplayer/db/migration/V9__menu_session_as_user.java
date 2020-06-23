package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Give old incomplete forms a default value
 */
public class V9__menu_session_as_user extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("ALTER TABLE menu_sessions " +
                "ADD asUser VARCHAR");
    }
}
