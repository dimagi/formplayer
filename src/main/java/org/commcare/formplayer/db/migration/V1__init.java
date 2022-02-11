package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Initialize the formplayer session database
 *
 * @author wspride
 */
public class V1__init extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("CREATE TABLE formplayer_sessions (\n" +
                "    id text NOT NULL,\n" +
                "    instancexml text,\n" +
                "    formxml text,\n" +
                "    restorexml text,\n" +
                "    username text,\n" +
                "    initlang text,\n" +
                "    sequenceid text,\n" +
                "    domain text,\n" +
                "    posturl text,\n" +
                "    sessiondata bytea,\n" +
                "    CONSTRAINT sessions_pkey PRIMARY KEY (id)\n" +
                ")"
        );
    }
}
