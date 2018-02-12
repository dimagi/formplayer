package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by willpride on 11/9/17.
 */
public class V16__add_prompt_nav_mode implements SpringJdbcMigration {
    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
                "ADD inPromptMode boolean DEFAULT false");
    }
}
