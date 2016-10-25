package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Give old incomplete forms a default value
 */
public class V7__menu_session_as_user implements SpringJdbcMigration {
    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("ALTER TABLE menu_sessions " +
                "ADD asUser VARCHAR");
    }
}
