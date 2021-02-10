package org.commcare.formplayer.db.migration;

import java.util.Arrays;

public class V19__form_session_correctly_typed_fields extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "ALTER TABLE formplayer_sessions ADD COLUMN datecreated timestamp with time zone",
                "CREATE INDEX ON formplayer_sessions (datecreated)",
        );
    }
}
