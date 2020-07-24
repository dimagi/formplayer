package org.commcare.formplayer.application;

import org.commcare.formplayer.aspects.*;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.commcare.formplayer.engine.FormplayerArchiveFileRoot;
import org.commcare.formplayer.installers.FormplayerInstallerFactory;
import io.sentry.SentryClientFactory;
import io.sentry.dsn.InvalidDsnException;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.repo.MenuSessionRepo;
import org.commcare.formplayer.repo.impl.PostgresFormSessionRepo;
import org.commcare.formplayer.repo.impl.PostgresMenuSessionRepo;
import org.commcare.formplayer.services.*;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerSentry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

//have to exclude this to use two DataSources (HQ and Formplayer dbs)
@EnableAutoConfiguration
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"application.*", "repo.*", "objects.*", "requests.*", "session.*", "installers.*"})
@EnableAspectJAutoProxy
public class WebAppContext implements WebMvcConfigurer {

    @Value("${commcarehq.environment}")
    private String environment;

    @Value("${commcarehq.host}")
    private String hqHost;

    @Value("${redis.hostname:#{null}}")
    private String redisHostName;

    @Value("${redis.clusterString:#{null}}")
    private String redisClusterString;

    @Value("${redis.password:#{null}}")
    private String redisPassword;

    @Value("${sentry.dsn:}")
    private String ravenDsn;

    @Value("${sqlite.optionsString:?journal_mode=MEMORY}")
    private String sqliteOptionsString;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        SqlSandboxUtils.optionsString = sqliteOptionsString;
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
    public JedisConnectionFactory jedisConnFactory(){
        if (redisClusterString != null) {
            List<String> nodeList = Arrays.asList(redisClusterString.split(","));
            RedisClusterConfiguration config = new RedisClusterConfiguration(nodeList);
            config.setPassword(redisPassword);
            return new JedisConnectionFactory(config);
        } else {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHostName);
            config.setPassword(redisPassword);
            return new JedisConnectionFactory(config);
        }
    }

    @Bean
    public FormplayerLockRegistry userLockRegistry() {
        return new FormplayerLockRegistry();
    }

    @Bean
    public StringRedisTemplate redisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate(jedisConnFactory());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplateString() {
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
    public RedisTemplate<String, FormVolatilityRecord> redisVolatilityDict() {
        RedisTemplate template = new RedisTemplate<String, FormVolatilityRecord>();
        template.setConnectionFactory(jedisConnFactory());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public FormSessionRepo formSessionRepo(){
        return new PostgresFormSessionRepo();
    }

    @Bean
    public MenuSessionRepo menuSessionRepo(){
        return new PostgresMenuSessionRepo();
    }

    @Bean
    public XFormService xFormService(){
        return new XFormService();
    }

    @Bean(destroyMethod="cleanup")
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public FormplayerSentry raven() {
        FormplayerSentry raven;
        try {
            raven = new FormplayerSentry(SentryClientFactory.sentryClient(ravenDsn));
        } catch (InvalidDsnException e) {
            raven = new FormplayerSentry(null);
        }
        raven.newBreadcrumb()
                .setData("environment", environment)
                .record();
        return raven;
    }

    @Bean
    public FallbackSentryReporter sentryReporter() { return new FallbackSentryReporter(); }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public CategoryTimingHelper categoryTimingHelper() {
        return new CategoryTimingHelper();
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
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public InstallService installService(){
        return new InstallService();
    }

    @Bean FormattedQuestionsService formattedQuestionsService() {
        return new FormattedQuestionsService();
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
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
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    BrowserValuesProvider browserValuesProvider() {
        return new BrowserValuesProvider();
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
    public ConfigureStorageFromSessionAspect configureStorageAspect() {
        return new ConfigureStorageFromSessionAspect();
    }

    @Bean
    public SetBrowserValuesAspect setBrowserValuesAspect() {
        return new SetBrowserValuesAspect();
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
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(Constants.CONNECT_TIMEOUT))
                .setReadTimeout(Duration.ofMillis(Constants.READ_TIMEOUT));
    }

    @Bean
    public FormplayerFormSendCalloutHandler formSendCalloutHandler() {
        return new FormplayerFormSendCalloutHandler();
    }

    @Bean
    public MenuSessionRunnerService menuSessionRunnerService() {return new MenuSessionRunnerService();}

    @Bean
    public MenuSessionFactory menuSessionFactory() {return new MenuSessionFactory();}
}
