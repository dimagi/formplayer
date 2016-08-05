package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Initialize the formplayer session database
 * @author wspride
 */
public class V2__init_menu_session implements SpringJdbcMigration {

    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("CREATE TABLE menu_sessions (\n" +
                "    id text NOT NULL,\n" +
                "    username text,\n" +
                "    domain text,\n" +
                "    appid text,\n" +
                "    installreference text,\n" +
                "    locale text,\n" +
                "    commcaresession bytea,\n" +
                "    CONSTRAINT menu_sessions_pkey PRIMARY KEY (id)\n" +
                ")");
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions ADD menu_session_id text");
    }
}