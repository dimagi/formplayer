package repo;

import org.springframework.boot.autoconfigure.jdbc.TomcatDataSourceConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DataSource configuration for accessing formplayer PostgresDB
 * Corresponds to spring.ds_formplayer.* application.properties
 */
@Configuration
@ConfigurationProperties(value = "spring.ds_formplayer")
public class FormplayerPostgresConfig extends TomcatDataSourceConfiguration {

    @Bean(name = "dsFormplayer")
    public DataSource dataSource() {
        return super.dataSource();
    }

    @Bean(name = "jdbcFormplayer")
    public JdbcTemplate jdbcTemplate(DataSource dsFormplayer) {
        System.out.println("Returning Formplayer JDBC template: " + this.getUrl());
        return new JdbcTemplate(dsFormplayer);
    }
}