package db.migrations;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by willpride on 6/30/16.
 */
public class V1_0__Initialize implements SpringJdbcMigration {
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute(
                "CREATE TABLE formplayer_sessions\n" +
                "(\n" +
                "  id text NOT NULL,\n" +
                "  instancexml text,\n" +
                "  formxml text,\n" +
                "  restorexml text,\n" +
                "  username text,\n" +
                "  initlang text,\n" +
                "  sequenceid text,\n" +
                "  domain text,\n" +
                "  posturl text,\n" +
                "  sessiondata bytea,\n" +
                "  CONSTRAINT sessions_pkey PRIMARY KEY (id)\n" +
                ")");
    }
}