package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Give old incomplete forms a default value
 */
public class V6__formsessions_oqps implements SpringJdbcMigration {
    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
                "ADD oneQuestionPerScreen boolean DEFAULT false");
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
                "ADD currentIndex integer DEFAULT -1");
    }
}
