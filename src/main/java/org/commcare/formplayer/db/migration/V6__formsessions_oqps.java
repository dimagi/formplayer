package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Give old incomplete forms a default value
 */
public class V6__formsessions_oqps extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "ALTER TABLE formplayer_sessions " +
                        "ADD oneQuestionPerScreen boolean DEFAULT false",

                "ALTER TABLE formplayer_sessions " +
                        "ADD currentIndex integer DEFAULT -1"
        );
    }
}
