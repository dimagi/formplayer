package org.commcare.formplayer.db.migration;

import java.util.Arrays;

public class V21__add_form_session_submit_status extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "ALTER TABLE formplayer_sessions ADD COLUMN submit_status varchar(30)"
        );
    }
}
