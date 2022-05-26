package org.commcare.formplayer.db.migration;

import org.commcare.formplayer.util.Constants;

import java.util.Arrays;

public class V24__virtual_data_instance_key extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        String table = Constants.POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME;
        return Arrays.asList(
                "ALTER TABLE " + table + " ADD COLUMN key varchar",
                "UPDATE " + table + " SET key = id",
                "ALTER TABLE " + table + " ALTER COLUMN key SET NOT NULL",
                "CREATE UNIQUE INDEX ON " + table + " (key)"
        );
    }
}
