package org.commcare.formplayer.repo.impl;

import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@Transactional
public class PostgresFormSessionRepoTest {

    @Autowired
    FormSessionRepo formSessionRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    public void cleanup() {
        cacheManager.getCache("form_session").clear();
    }

    @Test
    public void testFindOneWrapped_NotFound() {
        assertThrows(FormNotFoundException.class, () -> formSessionRepo.findOneWrapped("123"));
    }

    @Test
    public void testFindOneWrapped_cached() {
        // no cache to start
        assertEquals(Optional.empty(), getCachedSession("123"));

        // save a session
        SerializableFormSession session = new SerializableFormSession("123");
        session.setSequenceId(1);
        formSessionRepo.save(session);

        // cache is populated on save
        assertEquals(1, getCachedSession("123").get().getSequenceId());

        // find works
        session = formSessionRepo.findOneWrapped("123");
        assertEquals(1, session.getSequenceId());
        assertEquals(session, getCachedSession("123").get());

        // update session
        session.setSequenceId(2);
        formSessionRepo.save(session);

        // cache and find return updated session
        assertEquals(2, getCachedSession("123").get().getSequenceId());
        assertEquals(2, formSessionRepo.findOneWrapped("123").getSequenceId());

        // ensure update is persisted to DB
        cacheManager.getCache("form_session").clear();
        assertEquals(2, formSessionRepo.findOneWrapped("123").getSequenceId());
    }

    @Test
    public void testDeleteClearsCache() {
        SerializableFormSession session = new SerializableFormSession("123");
        formSessionRepo.save(session);
        assertEquals(session, getCachedSession("123").get());

        formSessionRepo.delete(session);
        assertEquals(Optional.empty(), getCachedSession("123"));
    }

    @Test
    public void testDeleteByIdClearsCache() {
        SerializableFormSession session = new SerializableFormSession("123");
        formSessionRepo.save(session);
        assertEquals(session, getCachedSession("123").get());

        formSessionRepo.deleteById(session.getId());
        assertEquals(Optional.empty(), getCachedSession("123"));
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
