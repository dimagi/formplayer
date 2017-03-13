package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by benrudolph on 3/13/17.
 */
public class V12__drop_restore_xml_column implements SpringJdbcMigration {
    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute(
                "ALTER TABLE formplayer_sessions DROP COLUMN restoreXml"
        );
    }
}
