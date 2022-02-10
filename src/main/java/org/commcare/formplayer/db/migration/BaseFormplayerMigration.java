package org.commcare.formplayer.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public abstract class BaseFormplayerMigration extends BaseJavaMigration {
    @Override
    public void migrate(Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(
                new SingleConnectionDataSource(context.getConnection(), true));
        for (String sql : getSqlStatements()) {
            jdbcTemplate.execute(sql);
        }
    }

    public abstract Iterable<String> getSqlStatements();
}
