package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Created by benrudolph on 3/13/17.
 */
public class V12__drop_restore_xml_column extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "ALTER TABLE formplayer_sessions DROP COLUMN restoreXml"
        );
    }
}
