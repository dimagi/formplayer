package org.commcare.formplayer.db.migration;

import org.commcare.formplayer.util.Constants;

import java.util.Arrays;

public class V23__init_virtual_data_instance extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "CREATE TABLE " + Constants.POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME + " (\n" +
                        "    id uuid NOT NULL,\n" +
                        "    username varchar NOT NULL,\n" +
                        "    asUser varchar,\n" +
                        "    domain varchar NOT NULL,\n" +
                        "    appid varchar NOT NULL,\n" +
                        "    instanceid varchar NOT NULL,\n" +
                        "    reference varchar NOT NULL,\n" +
                        "    instancexml bytea NOT NULL,\n" +
                        "    datecreated timestamptz NOT NULL,\n" +
                        "    CONSTRAINT virtual_data_instance_pkey PRIMARY KEY (id)\n" +
                        ")",
                "CREATE INDEX ON " + Constants.POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME + " (datecreated)"
        );
    }
}
