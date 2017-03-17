package application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import util.PrototypeUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableWebMvc
@Component
@EnableJpaRepositories(basePackages = {"repo.*", "objects.*"})
@EntityScan("objects.*")
public class Application {

    private final Log log = LogFactory.getLog(Application.class);

    static DataSource dataSource;

    public static void main(String[] args) {
        PrototypeUtils.setupPrototypes();
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        migrate();
    }

    /**
     * Attempt to run any outstanding migration in migration
     * Does nothing if DB is up to date
     */
    private static void migrate() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    // automatically pulls the @Primary DataSource from WebAppContext
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * This filter intercepts responses before they're dispatched and logs the request URL and response status
     */
    private class ResponseLoggingFilter extends GenericFilterBean {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            final HttpServletResponse httpResponse = (HttpServletResponse) response;
            log.info("Got request URL: " + httpRequest.getRequestURL() + " , response code: " + httpResponse.getStatus());
            filterChain.doFilter(request, response);
        }
    }

    // Autowire the filter above
    @Bean
    public Filter loggingFilter() {
        return new ResponseLoggingFilter();
    }
}