package application;

import aspects.*;
import com.getsentry.raven.RavenFactory;
import com.getsentry.raven.dsn.InvalidDsnException;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import engine.FormplayerArchiveFileRoot;
import installers.FormplayerInstallerFactory;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
import repo.impl.PostgresFormSessionRepo;
import repo.impl.PostgresMenuSessionRepo;
import repo.impl.PostgresMigratedFormSessionRepo;
import repo.impl.PostgresUserRepo;
import services.*;
import util.Constants;
import util.FormplayerRaven;

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

    @Value("${redis.hostname}")
    private String redisHostName;

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
        ds.setRemoveAbandoned(true);
        ds.setTestOnBorrow(true);
        ds.setValidationQuery("select 1");
        return ds;
    }
    @Bean
    public DataSource hqDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ds.setDriverClassName(hqPostgresDriverName);
        ds.setUrl(hqPostgresUrl);
        ds.setUsername(hqPostgresUsername);
        ds.setPassword(hqPostgresPassword);
        ds.setRemoveAbandoned(true);
        ds.setTestOnBorrow(true);
        ds.setValidationQuery("select 1");
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
    public PostgresUserRepo postgresUserRepo(){
        return new PostgresUserRepo();
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
        return new XFormService();
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public FormplayerRaven raven() {
        FormplayerRaven raven;
        try {
            raven = new FormplayerRaven(RavenFactory.ravenInstance(ravenDsn));
        } catch (InvalidDsnException e) {
            raven = new FormplayerRaven(null);
        }
        raven.newBreadcrumb()
                .setData("environment", environment)
                .record();
        return raven;
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
        return new InstallService();
    }

    @Bean FormattedQuestionsService formattedQuestionsService() {
        return new FormattedQuestionsService();
    }

    @Bean
    public SubmitService submitService(){
        return new SubmitService();
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
        return new QueryRequester();
    }

    @Bean
    public SyncRequester syncRequester() {
        return new SyncRequester();
    }

    @Bean
    public HqUserDetailsService userDetailsService(RestTemplateBuilder builder) { return new HqUserDetailsService(builder); }

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }
}