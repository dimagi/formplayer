package org.commcare.formplayer.db.migration;

import java.util.Arrays;

public class V18__init_shedlock extends BaseFormplayerMigration {

    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("CREATE TABLE shedlock (\n" +
                "    name VARCHAR(64) NOT NULL,\n" +
                "    lock_until TIMESTAMP NOT NULL,\n" +
                "    locked_at TIMESTAMP NOT NULL,\n" +
                "    locked_by VARCHAR(255) NOT NULL,\n" +
                "    PRIMARY KEY (name)\n" +
                ")"
        );
    }
}
