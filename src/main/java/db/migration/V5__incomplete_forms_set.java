package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Give old incomplete forms a default value
 */
public class V5__incomplete_forms_set implements SpringJdbcMigration {
    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("UPDATE formplayer_sessions " +
                "SET title='CommCare Form' WHERE title IS NULL");
        jdbcTemplate.execute("UPDATE formplayer_sessions " +
                "SET dateOpened='Thu Jan 1 00:00:00 UTC 1970' WHERE dateOpened IS NULL");
    }
}
