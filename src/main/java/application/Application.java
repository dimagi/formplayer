package application;

import objects.SerializableSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import repo.SessionRepo;
import repo.impl.SessionImpl;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        SessionRepo repo = (SessionRepo) context.getBean("sessionRepo");

        SerializableSession session = new SerializableSession();
        session.setId("will");
        session.setInstanceXml("<test/>");

        repo.save(session);

        System.out.println("Saved!");

        SerializableSession session2 = repo.find("will");

        System.out.println("Got: " + session2);

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
}