package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by willpride on 11/9/17.
 */
public class V17__add_case_id_field implements SpringJdbcMigration {
    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
                "ADD caseId VARCHAR DEFAULT NULL");
    }
}
