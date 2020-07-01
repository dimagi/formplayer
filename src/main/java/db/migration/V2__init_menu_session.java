package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Initialize the formplayer session database
 * @author wspride
 */
public class V2__init_menu_session extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "CREATE TABLE menu_sessions (\n" +
                    "    id text NOT NULL,\n" +
                    "    username text NOT NULL,\n" +
                    "    domain text NOT NULL,\n" +
                    "    appid text NOT NULL,\n" +
                    "    installreference text NOT NULL,\n" +
                    "    locale text,\n" +
                    "    commcaresession bytea NOT NULL,\n" +
                    "    CONSTRAINT menu_sessions_pkey PRIMARY KEY (id)\n" +
                    ")",

                "ALTER TABLE formplayer_sessions ADD menu_session_id text",

                "ALTER TABLE formplayer_sessions " +
                        "ADD FOREIGN KEY (menu_session_id) " +
                        "REFERENCES menu_sessions(id)"
        );
    }
}