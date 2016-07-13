package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Initialize the formplayer session database
 * @author wspride
 */
public class V1__init implements SpringJdbcMigration {

    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("CREATE TABLE formplayer_sessions (\n" +
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
                ")");
    }
}