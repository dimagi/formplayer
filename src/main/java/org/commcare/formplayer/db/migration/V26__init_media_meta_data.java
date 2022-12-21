package org.commcare.formplayer.db.migration;

import org.commcare.formplayer.util.Constants;

import java.util.Arrays;

public class V26__init_media_meta_data extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList(
                "CREATE TABLE " + Constants.POSTGRES_MEDIA_META_DATA_TABLE_NAME + " (\n" +
                        "    id varchar NOT NULL,\n" +
                        "    filepath varchar NOT NULL,\n" +
                        "    formsessionid varchar,\n" +
                        "    CONSTRAINT formSessionId_constraint FOREIGN KEY (formSessionId) REFERENCES formplayer_sessions (id) ON DELETE SET NULL,\n" +
                        "    contenttype varchar,\n" +
                        "    username varchar NOT NULL,\n" +
                        "    asuser varchar,\n" +
                        "    domain varchar NOT NULL,\n" +
                        "    appid varchar NOT NULL,\n" +
                        "    datecreated timestamptz NOT NULL,\n" +
                        "    contentlength integer\n" +
                        ")",
                "CREATE INDEX ON " + Constants.POSTGRES_MEDIA_META_DATA_TABLE_NAME + " (datecreated)"
        );
    }
}
