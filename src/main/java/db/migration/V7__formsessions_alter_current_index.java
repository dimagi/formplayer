package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Changes the Current Index field to be VARCHAR instead of INTEGER
 */
public class V7__formsessions_alter_current_index implements SpringJdbcMigration {
  @Override
  public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
    jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
        "ADD COLUMN temp_currentIndex text");
    jdbcTemplate.execute("UPDATE formplayer_sessions " +
        "SET temp_currentIndex = cast(cast(currentIndex as varchar) as text)");
    jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
        "DROP COLUMN currentIndex");
    jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
        "ADD COLUMN currentIndex text DEFAULT '-1'");
    jdbcTemplate.execute("UPDATE formplayer_sessions " +
        "SET currentIndex = temp_currentIndex");
    jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
        "DROP COLUMN temp_currentIndex");
  }
}
