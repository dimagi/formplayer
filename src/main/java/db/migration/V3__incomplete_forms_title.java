package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Initialize the formplayer session database
 * @author wspride
 */
public class V3__incomplete_forms_title implements SpringJdbcMigration {

    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
                "ADD title text");
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
                "ADD dateOpened text");
    }
}