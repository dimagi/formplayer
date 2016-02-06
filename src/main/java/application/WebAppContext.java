package application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
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
import services.RestoreService;
import services.XFormService;
import services.impl.RestoreServiceImpl;
import services.impl.XFormServiceImpl;

import java.util.Properties;

@Configuration
@EnableWebMvc
public class WebAppContext extends WebMvcConfigurerAdapter {

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
        SessionImpl impl = new SessionImpl();
        return impl;
    }

    @Bean
    public MenuRepo menuRepo(){
        MenuImpl impl = new MenuImpl();
        return impl;
    }
    @Bean
    public XFormService xFormService(){
        XFormServiceImpl impl = new XFormServiceImpl();
        return impl;
    }

    @Bean
    public RestoreService restoreService(){
        RestoreService impl = new RestoreServiceImpl();
        return impl;
    }
}