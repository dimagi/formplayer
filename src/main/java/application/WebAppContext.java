package application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.mail.MailSender;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import repo.MenuRepo;
import repo.SessionRepo;
import repo.impl.MenuImpl;
import repo.impl.SessionImpl;
import services.InstallService;
import services.RestoreService;
import services.XFormService;
import services.impl.InstallServiceImpl;
import services.impl.RestoreServiceImpl;
import services.impl.XFormServiceImpl;

import javax.annotation.PreDestroy;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

@Configuration
@EnableWebMvc
public class WebAppContext extends WebMvcConfigurerAdapter {

    Log log = LogFactory.getLog(WebAppContext.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("/static/");
    }

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
    public JedisConnectionFactory jedisConnFactory(){
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setUsePool(true);
        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate redisTemplate(){
        RedisTemplate redisTemplate =  new RedisTemplate();
        redisTemplate.setConnectionFactory(jedisConnFactory());
        return redisTemplate;
    }

    @Bean
    public SessionRepo sessionRepo(){
        return new SessionImpl();
    }

    @Bean
    public MenuRepo menuRepo(){
        return new MenuImpl();
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
        return new InstallServiceImpl();
    }

    @Bean
    public LockRegistry formSessionLockRegistry() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        return new RedisLockRegistry(jedisConnectionFactory, "form-session");
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