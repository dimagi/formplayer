package db.migration;

        import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
        import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Add default values to new incomplete forms columns
 * @author wspride
 */
public class V4__incomplete_forms_default implements SpringJdbcMigration {

    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
                "ALTER title SET DEFAULT 'CommCare Form'");
        jdbcTemplate.execute("ALTER TABLE formplayer_sessions " +
                "ALTER dateOpened SET DEFAULT 'Tue Jul 3 14:19:24 EDT 1990'");
    }
}