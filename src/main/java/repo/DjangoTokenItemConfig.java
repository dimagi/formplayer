package repo;

import org.springframework.boot.autoconfigure.jdbc.TomcatDataSourceConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DataSource configuration for accessing commcarehq PostgresDB
 * Corresponds to spring.ds_hq.* application.properties
 */
@Configuration
@ConfigurationProperties(value = "spring.ds_hq")
public class DjangoTokenItemConfig extends TomcatDataSourceConfiguration {

    @Bean(name = "dsHq")
    public DataSource dataSource() {
        return super.dataSource();
    }

    @Bean(name = "jdbcHq")
    public JdbcTemplate jdbcTemplate(DataSource dsHq) {
        return new JdbcTemplate(dsHq);
    }
}