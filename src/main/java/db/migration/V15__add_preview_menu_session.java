package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by willpride on 11/9/17.
 */
public class V15__add_preview_menu_session implements SpringJdbcMigration {
    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("ALTER TABLE menu_sessions " +
                "ADD preview boolean DEFAULT false");
    }
}
