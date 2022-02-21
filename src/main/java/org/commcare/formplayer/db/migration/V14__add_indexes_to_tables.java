package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Adds indexes to commonly queried columns.
 */
public class V14__add_indexes_to_tables extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "CREATE INDEX formplayer_sessions_username_index ON formplayer_sessions (username)",
                "CREATE INDEX formplayer_sessions_id_index ON formplayer_sessions (id)",
                "CREATE INDEX menu_sessions_id_index ON menu_sessions (id)"
        );
    }
}
