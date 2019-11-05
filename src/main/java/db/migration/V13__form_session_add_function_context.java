package db.migration;

import java.util.Arrays;

/**
 * Add appId to form session
 */
public class V13__form_session_add_function_context extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("ALTER TABLE formplayer_sessions " +
                "ADD functioncontext bytea");
    }
}
