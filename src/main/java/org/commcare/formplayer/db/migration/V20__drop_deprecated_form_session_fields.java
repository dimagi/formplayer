package org.commcare.formplayer.db.migration;

import java.util.Arrays;

public class V20__drop_deprecated_form_session_fields extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "UPDATE formplayer_sessions\n" +
                        "    SET version = sequenceid::integer,\n" +
                        "        datecreated = dateopened::timestamp\n" +
                        "    WHERE datecreated is null",
                "ALTER TABLE formplayer_sessions DROP COLUMN dateopened",
                "ALTER TABLE formplayer_sessions DROP COLUMN sequenceid"
        );
    }
}
