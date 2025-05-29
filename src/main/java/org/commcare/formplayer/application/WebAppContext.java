package org.commcare.formplayer.application;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

import org.commcare.formplayer.aspects.AppInstallAspect;
import org.commcare.formplayer.aspects.ConfigureStorageFromSessionAspect;
import org.commcare.formplayer.aspects.LockAspect;
import org.commcare.formplayer.aspects.LoggingAspect;
import org.commcare.formplayer.aspects.MetricsAspect;
import org.commcare.formplayer.aspects.SetBrowserValuesAspect;
import org.commcare.formplayer.aspects.TagTracingDisabledAspect;
import org.commcare.formplayer.aspects.UserRestoreAspect;
import org.commcare.formplayer.engine.FormplayerArchiveFileRoot;
import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.services.BrowserValuesProvider;
import org.commcare.formplayer.services.FormattedQuestionsService;
import org.commcare.formplayer.services.FormplayerLockRegistry;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;


//have to exclude this to use two DataSources (HQ and Formplayer dbs)
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"org.commcare.formplayer.application.*",
        "org.commcare.formplayer.repo.*",
        "org.commcare.formplayer.objects.*",
        "org.commcare.formplayer.requests.*",
        "org.commcare.formplayer.session.*",
        "org.commcare.formplayer.installers.*"})
@EnableAspectJAutoProxy
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class WebAppContext implements WebMvcConfigurer {

    @Value("${redis.hostname:#{null}}")
    private String redisHostName;

    @Value("${redis.clusterString:#{null}}")
    private String redisClusterString;

    @Value("${redis.password:#{null}}")
    private String redisPassword;

    @Value("${detailed_tagging.tag_names:}")
    private List<String> detailedTagNames;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public StatsDClient datadogStatsDClient() {
        return new NonBlockingStatsDClientBuilder()
                .prefix("formplayer.metrics")
                .hostname("localhost")
                .port(8125)
                .build();
    }

    @Bean
    public MeterFilter applyMetricPrefix() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                return id.withName("formplayer." + id.getName());
            }
        };
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
    public RedisTemplate<String, String> redisSetTemplate() {
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
    @Scope(value= "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public FormplayerDatadog datadog() {
        FormplayerDatadog datadog = new FormplayerDatadog(datadogStatsDClient(), detailedTagNames);
        return datadog;
    }

    @Bean FormattedQuestionsService formattedQuestionsService() {
        return new FormattedQuestionsService();
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
    public TagTracingDisabledAspect tagTracingDisabledAspect() {
        return new TagTracingDisabledAspect();
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
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime() // Works on Postgres, MySQL, MariaDb, MS SQL, Oracle, DB2, HSQL and H2
                        .build()
        );
    }

    @Bean public RequestContextListener requestContextListener(){
        return new RequestContextListener();
    }
}
