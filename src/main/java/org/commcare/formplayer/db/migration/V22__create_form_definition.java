package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * A table to hold a serialized FormDef objects that can be shared across sessions
 * Uniquely identifiable by the combination of app id, form xmlns, and form version
 * See SerializableFormDefinition for the Java representation
 */
public class V22__create_form_definition extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "CREATE TABLE form_definition (\n" +
                        "    id bigserial PRIMARY KEY,\n" +
                        "    datecreated timestamp with time zone,\n" +
                        "    appid text NOT NULL,\n" +
                        "    formxmlns text NOT NULL,\n" +
                        "    formversion text NOT NULL,\n" +
                        "    formdef text NOT NULL,\n" +
                        "    CONSTRAINT form_definition_version UNIQUE (appid, formxmlns, formversion)\n" +
                        ")",

                "ALTER TABLE formplayer_sessions ADD form_definition_id bigint",

                "ALTER TABLE formplayer_sessions " +
                        "ADD FOREIGN KEY (form_definition_id) " +
                        "REFERENCES form_definition(id)"
        );
    }
}
