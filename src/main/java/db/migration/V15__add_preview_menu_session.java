package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Created by willpride on 11/9/17.
 */
public class V15__add_preview_menu_session extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("ALTER TABLE menu_sessions " +
                "ADD preview boolean DEFAULT false");
    }
}
