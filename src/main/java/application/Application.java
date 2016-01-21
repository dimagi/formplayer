package application;

import objects.SerializableSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import repo.SessionRepo;
import repo.impl.SessionImpl;
import services.RestoreService;
import services.XFormService;
import services.impl.RestoreServiceImpl;
import services.impl.XFormServiceImpl;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableWebMvc
@Component
public class Application {

    @Value("${touchforms.host")
    public static String HOST;

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
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
        impl.setRedisTemplate(redisTemplate());
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