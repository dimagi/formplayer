package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Add appVersion to form session
 */
public class V28__form_session_add_app_version extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("ALTER TABLE formplayer_sessions " +
                "ADD appversion VARCHAR DEFAULT NULL");
    }
}
