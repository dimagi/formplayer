package application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.lightcouch.CouchDbClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.TokenRepo;
import repo.impl.*;
import services.*;
import services.impl.InstallServiceImpl;
import services.impl.SubmitServiceImpl;
import services.impl.XFormServiceImpl;
import util.Constants;

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
@ComponentScan(basePackages = {"application.*", "repo.*", "objects.*", "requests.*", "session.*"})
public class WebAppContext extends WebMvcConfigurerAdapter {

    @Value("${commcarehq.host}")
    private String hqHost;

    @Value("${datasource.hq.url}")
    private String hqPostgresUrl;

    @Value("${datasource.hq.username}")
    private String hqPostgresUsername;

    @Value("${datasource.hq.password}")
    private String hqPostgresPassword;

    @Value("${datasource.hq.driverClassName}")
    private String hqPostgresDriverName;

    @Value("${datasource.formplayer.url}")
    private String formplayerPostgresUrl;

    @Value("${datasource.formplayer.username}")
    private String formplayerPostgresUsername;

    @Value("${datasource.formplayer.password}")
    private String formplayerPostgresPassword;

    @Value("${datasource.formplayer.driverClassName}")
    private String formplayerPostgresDriverName;

    @Value("${smtp.host}")
    private String smtpHost;

    @Value("${smtp.port}")
    private int smtpPort;

    @Value("${smtp.username}")
    private String smtpUsername;

    @Value("${smtp.password}")
    private String smtpPassword;

    @Value("${smtp.from.address}")
    private String smtpFromAddress;

    @Value("${smtp.to.address}")
    private String smtpToAddress;

    @Value("${redis.hostname}")
    private String redisHostName;

    @Value("${couch.protocol}")
    private String couchProtocol;

    @Value("${couch.host}")
    private String couchHost;

    @Value("${couch.port}")
    private int couchPort;

    @Value("${couch.username}")
    private String couchUsername;

    @Value("${couch.password}")
    private String couchPassword;

    @Value("${couch.databaseName}")
    private String couchDatabaseName;

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
    public DataSource formplayerDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ds.setDriverClassName(formplayerPostgresDriverName);
        ds.setUrl(formplayerPostgresUrl);
        ds.setUsername(formplayerPostgresUsername);
        ds.setPassword(formplayerPostgresPassword);
        return ds;
    }
    @Bean
    public DataSource hqDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ds.setDriverClassName(hqPostgresDriverName);
        ds.setUrl(hqPostgresUrl);
        ds.setUsername(hqPostgresUsername);
        ds.setPassword(hqPostgresPassword);
        return ds;
    }

    @Bean
    public JedisConnectionFactory jedisConnFactory(){
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setUsePool(true);
        jedisConnectionFactory.setHostName(redisHostName);
        return jedisConnectionFactory;
    }

    @Bean
    public CouchDbClient userCouchDbClient() {
        return new CouchDbClient(
                couchDatabaseName + Constants.COUCH_USERS_DB,
                false,
                couchProtocol,
                couchHost,
                couchPort,
                couchUsername,
                couchPassword
        );
    }

    @Bean
    public RedisLockRegistry userLockRegistry() {
        JedisConnectionFactory jedisConnectionFactory = jedisConnFactory();
        return new RedisLockRegistry(jedisConnectionFactory, "formplayer-user");
    }

    @Bean
    public HtmlEmail exceptionMessage() throws EmailException {
        HtmlEmail message = new HtmlEmail();
        message.setFrom(smtpFromAddress);
        message.addTo(smtpToAddress);
        message.setAuthentication(smtpUsername, smtpPassword);
        message.setHostName(smtpHost);
        message.setSmtpPort(smtpPort);
        return message;
    }

    @Bean
    public PostgresUserRepo postgresUserRepo(){
        return new PostgresUserRepo();
    }

    @Bean
    public CouchUserRepo couchUserRepo(){
        return new CouchUserRepo();
    }

    @Bean
    public TokenRepo tokenRepo(){
        return new PostgresTokenRepo();
    }

    @Bean
    public FormSessionRepo formSessionRepo(){
        return new PostgresFormSessionRepo();
    }

    @Bean
    public FormSessionRepo migratedFormSessionRepo(){
        return new PostgresMigratedFormSessionRepo();
    }

    @Bean
    public MenuSessionRepo menuSessionRepo(){
        return new PostgresMenuSessionRepo();
    }

    @Bean
    public XFormService xFormService(){
        return new XFormServiceImpl();
    }

    @Bean
    public RestoreFactory restoreFactory(){
        return new RestoreFactory();
    }

    @Bean
    public InstallService installService(){
        return new InstallServiceImpl();
    }

    @Bean
    public SubmitService submitService(){
        return new SubmitServiceImpl();
    }

    @Bean
    public NewFormResponseFactory newFormResponseFactory(){
        return new NewFormResponseFactory(formSessionRepo(), xFormService(), restoreFactory());
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