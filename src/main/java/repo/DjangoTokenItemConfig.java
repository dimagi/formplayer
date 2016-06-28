package repo;

import org.springframework.boot.autoconfigure.jdbc.TomcatDataSourceConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConfigurationProperties(value = "spring.ds_hq")
public class DjangoTokenItemConfig extends TomcatDataSourceConfiguration {

    @Bean(name = "dsHq")
    public DataSource dataSource() {
        return super.dataSource();
    }

    @Bean(name = "jdbcHq")
    public JdbcTemplate jdbcTemplate(DataSource dsHq) {
        System.out.println("Returning HQ JDBC template");
        return new JdbcTemplate(dsHq);
    }
}