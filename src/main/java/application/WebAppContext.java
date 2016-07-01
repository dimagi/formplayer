package application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import repo.SessionRepo;
import repo.TokenRepo;
import repo.impl.PostgresSessionRepo;
import repo.impl.PostgresTokenRepo;
import services.InstallService;
import services.RestoreService;
import services.XFormService;
import services.impl.InstallServiceImpl;
import services.impl.RestoreServiceImpl;
import services.impl.XFormServiceImpl;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

//have to exclude this to use two DataSources (HQ and Formplayer dbs)
@EnableAutoConfiguration
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"application.*", "repo.*", "objects.*"})
@PropertySource(value="file:config/application.properties")
public class WebAppContext extends WebMvcConfigurerAdapter {

    @Value("${redis.hostname}")
    private String redisHostName;

    @Value("${commcarehq.host}")
    private String hqHost;

    private final Log log = LogFactory.getLog(WebAppContext.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("/static/");
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");}

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Bean
    public SimpleMappingExceptionResolver exceptionResolver() {
        SimpleMappingExceptionResolver exceptionResolver = new SimpleMappingExceptionResolver();

        Properties exceptionMappings = new Properties();

        exceptionMappings.put("java.lang.Exception", "error/error");
        exceptionMappings.put("java.lang.RuntimeException", "error/error");

        exceptionResolver.setExceptionMappings(exceptionMappings);

        Properties statusCodes = new Properties();

        statusCodes.put("error/404", "404");
        statusCodes.put("error/error", "500");

        exceptionResolver.setStatusCodes(statusCodes);

        return exceptionResolver;
    }

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setViewClass(JstlView.class);
        viewResolver.setPrefix("/WEB-INF/jsp/");
        viewResolver.setSuffix(".jsp");

        return viewResolver;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public JdbcTemplate formplayerTemplate(){
        return new JdbcTemplate(formplayerDataSource());
    }

    @Bean
    public JdbcTemplate hqTemplate(){
        return new JdbcTemplate(hqDataSource());
    }

    @Primary
    @Bean
    public static DataSource formplayerDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        String url = "jdbc:postgresql://localhost:5432/formplayer";
        String user = "flyway";
        String pass = "flyway";
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        return ds;
    }
    @Bean
    public static DataSource hqDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        String url = "jdbc:postgresql://localhost:5432/commcarehq";
        String user = "postgres";
        String pass = "123";
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        return ds;
    }


    @Bean
    public TokenRepo tokenRepo(){
        return new PostgresTokenRepo();
    }

    @Bean
    public SessionRepo sessionRepo(){
        return new PostgresSessionRepo();
    }

    @Bean
    public XFormService xFormService(){
        return new XFormServiceImpl();
    }

    @Bean
    public RestoreService restoreService(){
        return new RestoreServiceImpl();
    }

    @Bean
    public InstallService installService(){
        return new InstallServiceImpl(hqHost);
    }

    // Manually deregister drivers as prescribed here http://stackoverflow.com/questions/11872316/tomcat-guice-jdbc-memory-leak
    @PreDestroy
    public void deregisterDrivers(){
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                log.info(String.format("deregistering jdbc driver: %s", driver));
            } catch (SQLException e) {
                log.warn(String.format("Error deregistering driver %s", driver), e);
            }

        }
    }

}