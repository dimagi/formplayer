package org.commcare.formplayer.db.migration;

import java.util.Arrays;


/**
 * The formxml column has been replaced by the form_definition table/foreign key
 */
public class V25__drop_formxml_column extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "ALTER TABLE formplayer_sessions DROP COLUMN formxml"
        );
    }
}

