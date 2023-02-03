package org.commcare.formplayer.db.migration;

import org.commcare.formplayer.util.Constants;

import java.util.Arrays;

/**
 * Updates media_meta_data table with fileid column and assigns a value if filed is null
 */
public class V27_media_meta_data_fileid extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        String table = Constants.POSTGRES_MEDIA_META_DATA_TABLE_NAME;
        return Arrays.asList(
                "ALTER TABLE " + table + " ADD COLUMN fileid varchar",
                "UPDATE " + table + " SET fileid = id",
                "ALTER TABLE " + table + " ALTER COLUMN fileId SET NOT NULL",
                "CREATE UNIQUE INDEX ON " + table + " (fileid)"
        );
    }
}
