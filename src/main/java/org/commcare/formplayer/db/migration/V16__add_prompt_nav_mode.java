package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Created by willpride on 11/9/17.
 */
public class V16__add_prompt_nav_mode extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("ALTER TABLE formplayer_sessions " +
                "ADD inPromptMode boolean DEFAULT false");
    }
}
