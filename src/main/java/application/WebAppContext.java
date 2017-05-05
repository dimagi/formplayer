package application;

import aspects.*;
import com.getsentry.raven.Raven;
import com.getsentry.raven.RavenFactory;
import com.getsentry.raven.event.BreadcrumbBuilder;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import engine.FormplayerArchiveFileRoot;
import installers.FormplayerInstallerFactory;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.lightcouch.CouchDbClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
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
import services.impl.*;
import util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

//have to exclude this to use two DataSources (HQ and Formplayer dbs)
@EnableAutoConfiguration
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"application.*", "repo.*", "objects.*", "requests.*", "session.*", "installers.*"})
@EnableAspectJAutoProxy
public class WebAppContext extends WebMvcConfigurerAdapter {

    @Value("${commcarehq.environment}")
    private String environment;

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

    @Value("${smtp.username:}")
    private String smtpUsername;

    @Value("${smtp.password:}")
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

    @Value("${sentry.dsn:}")
    private String ravenDsn;

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
    public StatsDClient datadogStatsDClient() {
        return new NonBlockingStatsDClient(
                "formplayer.metrics",
                "localhost",
                8125
        );
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
        return new RedisLockRegistry(jedisConnectionFactory, "formplayer-user", Constants.LOCK_DURATION);
    }

    @Bean
    public StringRedisTemplate redisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate(jedisConnFactory());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisTemplate<String, Long> redisTemplateLong() {
        RedisTemplate template = new RedisTemplate<String, Long>();
        template.setConnectionFactory(jedisConnFactory());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public HtmlEmail exceptionMessage() throws EmailException {
        HtmlEmail message = new HtmlEmail();
        message.setFrom(smtpFromAddress);
        message.addTo(smtpToAddress);
        if (smtpUsername != null &&  smtpPassword != null) {
            message.setAuthentication(smtpUsername, smtpPassword);
        }
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
    @Qualifier(value = "migrated")
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
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Raven raven() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("environment", environment);
        BreadcrumbBuilder builder = new BreadcrumbBuilder();
        builder.setData(data);
        return RavenFactory.ravenInstance(ravenDsn);
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public RestoreFactory restoreFactory() {
        return new RestoreFactory();
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public FormplayerStorageFactory storageFactory() {
        return new FormplayerStorageFactory();
    }

    @Bean
    public InstallService installService(){
        return new InstallServiceImpl();
    }

    @Bean FormattedQuestionsService formattedQuestionsService() {
        return new FormattedQuestionsServiceImpl();
    }

    @Bean
    public SubmitService submitService(){
        return new SubmitServiceImpl();
    }

    @Bean
    public NewFormResponseFactory newFormResponseFactory(){
        return new NewFormResponseFactory();
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    FormplayerInstallerFactory installerFactory() {
        return new FormplayerInstallerFactory();
    }

    @Bean
    public LockAspect lockAspect() {
        return new LockAspect();
    }
    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }

    @Bean
    public MetricsAspect metricsAspect() {
        return new MetricsAspect();
    }

    @Bean
    public UserRestoreAspect userRestoreAspect() {
        return new UserRestoreAspect();
    }

    @Bean
    public AppInstallAspect appInstallAspect() {
        return new AppInstallAspect();
    }

    @Bean
    public ArchiveFileRoot formplayerArchiveFileRoot() {
        return new FormplayerArchiveFileRoot();
    }

    @Bean
    public QueryRequester queryRequester() {
        return new QueryRequesterImpl();
    }

    @Bean
    public SyncRequester syncRequester() {
        return new SyncRequesterImpl();
    }
}