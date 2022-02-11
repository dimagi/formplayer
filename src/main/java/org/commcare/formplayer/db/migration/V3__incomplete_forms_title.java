package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Initialize the formplayer session database
 *
 * @author wspride
 */
public class V3__incomplete_forms_title extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "ALTER TABLE formplayer_sessions ADD title text",
                "ALTER TABLE formplayer_sessions ADD dateOpened text"
        );
    }
}
