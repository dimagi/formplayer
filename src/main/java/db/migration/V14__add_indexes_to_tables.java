package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Adds indexes to commonly queried columns.
 */
public class V14__add_indexes_to_tables implements SpringJdbcMigration {

    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute(
                "CREATE INDEX formplayer_sessions_username_index ON formplayer_sessions (username)"
        );
        jdbcTemplate.execute(
                "CREATE INDEX formplayer_sessions_id_index ON formplayer_sessions (id)"
        );
        jdbcTemplate.execute(
                "CREATE INDEX menu_sessions_id_index ON menu_sessions (id)"
        );
    }
}
