package org.commcare.formplayer.services;

import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class FormSessionServiceTest {

    @Autowired
    FormSessionService formSessionService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private FormSessionRepo formSessionRepo;

    private String sessionId;

    @BeforeEach
    public void setUp() {
        // the repo always returns the saved object so simulate that in the mock
        when(formSessionRepo.save(any())).thenAnswer(new Answer<SerializableFormSession>() {
            @Override
            public SerializableFormSession answer(InvocationOnMock invocation) throws Throwable {
                return (SerializableFormSession)invocation.getArguments()[0];
            }
        });

        sessionId = UUID.randomUUID().toString();
    }

    @AfterEach
    public void cleanup() {
        cacheManager.getCache("form_session").clear();
    }

    @Test
    public void testGetSessionById_NotFound() {
        assertThrows(FormNotFoundException.class, () -> formSessionService.getSessionById(sessionId));
    }

    @Test
    public void testGetSessionById_cached() {
        // no cache to start
        assertEquals(Optional.empty(), getCachedSession(sessionId));

        // save a session
        SerializableFormSession session = new SerializableFormSession(sessionId);
        formSessionService.saveSession(session);

        // cache is populated on save
        assertEquals(session, getCachedSession(sessionId).get());

        // get session hits the cache (repo is mocked)
        SerializableFormSession fetchedSession = formSessionService.getSessionById(sessionId);
        assertEquals(session, fetchedSession);

        // update session
        session.setInstanceXml("xml1");
        formSessionService.saveSession(session);

        // cache and find return updated session
        assertEquals("xml1", getCachedSession(sessionId).get().getInstanceXml());
        assertEquals("xml1", formSessionService.getSessionById(sessionId).getInstanceXml());
    }

    @Test
    public void testDeleteSessionByIdEvictsFromCache() {
        SerializableFormSession session = new SerializableFormSession(sessionId);
        formSessionService.saveSession(session);
        assertEquals(session, getCachedSession(sessionId).get());

        formSessionService.deleteSessionById(session.getId());
        assertFalse(getCachedSession(sessionId).isPresent());
    }

    @Test
    public void testPurgeClearsCache() {
        formSessionService.saveSession(new SerializableFormSession(sessionId));
        assertTrue(getCachedSession(sessionId).isPresent());

        formSessionService.purge();
        assertFalse(getCachedSession(sessionId).isPresent());
    }

    private Optional<SerializableFormSession> getCachedSession(String sessionId) {
        return ofNullable(cacheManager.getCache("form_session")).map(
                c -> c.get(sessionId, SerializableFormSession.class)
        );
    }

    // only include the service under test and it's dependencies
    // This should not be necessary but we're using older versions of junit and spring
    @ComponentScan(
            basePackageClasses = {FormSessionService.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {FormSessionService.class})
    )
    @EnableCaching
    @Configuration
    public static class FormSessionServiceTestConfig {

        @MockBean
        public FormSessionRepo formSessionRepo;

        @MockBean
        public JdbcTemplate jdbcTemplate;

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("form_session");
        }
    }
}
