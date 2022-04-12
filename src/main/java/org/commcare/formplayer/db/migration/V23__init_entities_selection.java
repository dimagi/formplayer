package org.commcare.formplayer.db.migration;

import java.util.Arrays;

public class V23__init_entities_selection extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "CREATE TABLE entities_selection (\n" +
                        "    id uuid NOT NULL,\n" +
                        "    username varchar NOT NULL,\n" +
                        "    asUser varchar NOT NULL,\n" +
                        "    domain varchar NOT NULL,\n" +
                        "    appid varchar NOT NULL,\n" +
                        "    entities bytea NOT NULL,\n" +
                        "    datecreated timestamptz NOT NULL,\n" +
                        "    CONSTRAINT entities_selection_pkey PRIMARY KEY (id)\n" +
                        ")",
                "CREATE INDEX ON entities_selection (datecreated)"
        );
    }
}
