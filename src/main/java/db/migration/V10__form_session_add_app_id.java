package db.migration;

import java.util.Arrays;

/**
 * Add appId to form session
 */
public class V10__form_session_add_app_id extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("ALTER TABLE formplayer_sessions " +
                "ADD appid VARCHAR DEFAULT NULL");
    }
}
