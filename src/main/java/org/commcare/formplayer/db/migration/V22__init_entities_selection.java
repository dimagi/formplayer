package org.commcare.formplayer.db.migration;

import java.util.Arrays;

public class V22__init_entities_selection extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "CREATE TABLE entities_selection (\n" +
                        "    id text NOT NULL,\n" +
                        "    entities bytea NOT NULL,\n" +
                        "    CONSTRAINT entities_selection_pkey PRIMARY KEY (id)\n" +
                        ")"
        );
    }
}
