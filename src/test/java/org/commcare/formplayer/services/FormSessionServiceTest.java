package org.commcare.formplayer.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.FormSessionListViewRaw;
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
import org.springframework.util.SerializationUtils;

import java.time.Instant;
import java.util.*;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.commcare.formplayer.utils.JpaTestUtils.createProjection;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
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
                return (SerializableFormSession) invocation.getArguments()[0];
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
        session.setSequenceId(1);
        formSessionService.saveSession(session);

        // cache is populated on save
        assertEquals(1, getCachedSession(sessionId).get().getSequenceId());

        // get session hits the cache (repo is mocked)
        session = formSessionService.getSessionById(sessionId);
        assertEquals(1, session.getSequenceId());

        // update session
        session.setSequenceId(2);
        formSessionService.saveSession(session);

        // cache and find return updated session
        assertEquals(2, getCachedSession(sessionId).get().getSequenceId());
        assertEquals(2, formSessionService.getSessionById(sessionId).getSequenceId());
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

    @Test
    public void testGetSessionsForUser() {
        ImmutableMap<String, String> sessionData = ImmutableMap.of("a", "1", "b", "2");
        Map<String, Object> backingMap = new HashMap<>();
        backingMap.put("id", "Dave");
        backingMap.put("title", "Matthews");
        backingMap.put("dateOpened", new Date().toString());
        backingMap.put("dateCreated", Instant.now());
        backingMap.put("sessionData", SerializationUtils.serialize(sessionData));

        FormSessionListViewRaw rawView = createProjection(FormSessionListViewRaw.class, backingMap);

        when(formSessionRepo.findUserSessions(anyString())).thenReturn(ImmutableList.of(rawView));
        List<FormSessionListView> sessions = formSessionService.getSessionsForUser("username");

        HashMap<String, Object> expected = new HashMap<>(backingMap);
        expected.put("sessionData", sessionData);
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0)).extracting("id", "title", "dateOpened", "dateCreated", "sessionData")
                .containsAll(expected.values());
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
