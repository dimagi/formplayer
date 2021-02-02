package org.commcare.formplayer.repo.impl;

import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@Transactional
public class PostgresFormSessionRepoTest {

    @Autowired
    PostgresFormSessionRepo formSessionRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    CacheManager cacheManager;

    @After
    public void cleanup() {
        cacheManager.getCache("form_session").clear();
    }

    @Test(expected = FormNotFoundException.class)
    public void testFindOneWrapped_NotFound() {
        formSessionRepo.findOneWrapped("123");
    }

    @Test
    public void testFindOneWrapped_cached() {
        // no cache to start
        Assert.assertEquals(Optional.empty(), getCachedSession("123"));

        // save a session
        SerializableFormSession session = new SerializableFormSession("123");
        session.setAppId("app1");
        formSessionRepo.save(session);

        // cache is populated on save
        Assert.assertEquals("app1", getCachedSession("123").get().getAppId());

        // find works
        session = formSessionRepo.findOneWrapped("123");
        Assert.assertEquals("app1", session.getAppId());
        Assert.assertEquals(session, getCachedSession("123").get());

        // update session
        session.setAppId("app2");
        formSessionRepo.save(session);

        // cache and find return updated session
        Assert.assertEquals("app2", getCachedSession("123").get().getAppId());
        Assert.assertEquals("app2", formSessionRepo.findOneWrapped("123").getAppId());

    }

    @Test
    public void testDeleteClearsCache() {
        SerializableFormSession session = new SerializableFormSession("123");
        formSessionRepo.save(session);
        Assert.assertEquals(session, getCachedSession("123").get());

        formSessionRepo.delete(session);
        Assert.assertEquals(Optional.empty(), getCachedSession("123"));
    }

    @Test
    public void testDeleteByIdClearsCache() {
        SerializableFormSession session = new SerializableFormSession("123");
        formSessionRepo.save(session);
        Assert.assertEquals(session, getCachedSession("123").get());

        formSessionRepo.deleteById(session.getId());
        Assert.assertEquals(Optional.empty(), getCachedSession("123"));
    }

    private Optional<SerializableFormSession> getCachedSession(String id) {
        return ofNullable(cacheManager.getCache("form_session")).map(c -> c.get(id, SerializableFormSession.class));
    }

    @EnableCaching
    @Configuration
    @AutoConfigurationPackage
    public static class CachingTestConfig {

        @Bean
        public FormSessionRepo formSessionRepo() {
            return new PostgresFormSessionRepo();
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("form_session");
        }

    }
}
