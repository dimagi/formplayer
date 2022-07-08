package org.commcare.formplayer.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import static java.util.Optional.ofNullable;

import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.exceptions.MenuNotFoundException;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.repo.MenuSessionRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;


@ExtendWith(SpringExtension.class)
@ContextConfiguration
@EnableConfigurationProperties(value = CacheConfiguration.class)
@TestPropertySource("classpath:application.properties")
public class MenuSessionServiceTest {
    @Autowired
    MenuSessionService menuSessionService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private MenuSessionRepo menuSessionRepo;

    @BeforeEach
    public void setUp() {
        // the repo always returns the saved object so simulate that in the mock
        when(menuSessionRepo.save(any())).thenAnswer(new Answer<SerializableMenuSession>() {
            @Override
            public SerializableMenuSession answer(InvocationOnMock invocation) throws Throwable {
                SerializableMenuSession session = (SerializableMenuSession)invocation.getArguments()[0];
                if (session.getId() == null) {
                    // this is normally taken care of by Hibernate
                    ReflectionTestUtils.setField(session, "id", UUID.randomUUID().toString());
                }
                return session;
            }
        });
    }

    @AfterEach
    public void cleanup() {
        cacheManager.getCache("menu_session").clear();
    }

    @Test
    public void testCacheExists() {
        assertNotNull(cacheManager.getCache("menu_session"));
    }

    @Test
    public void testGetSessionById_NotFound() {
        assertThrows(MenuNotFoundException.class,
                () -> menuSessionService.getSessionById("123"));
    }

    @Test
    public void testGetSessionById_cached() {
        cacheManager.getCache("menu_session").clear();

        // save a session
        SerializableMenuSession session = new SerializableMenuSession();
        menuSessionService.saveSession(session);

        // cache is populated on save
        assertEquals(session, getCachedSession(session.getId()).get());

        // get session hits the cache (repo is mocked)
        SerializableMenuSession fetchedSession = menuSessionService.getSessionById(session.getId());
        assertEquals(session, fetchedSession);

        // update session
        byte[] bytes = {1, 2, 3};
        session.setCommcareSession(bytes);
        menuSessionService.saveSession(session);

        // cache and find return updated session
        assertEquals(bytes, getCachedSession(session.getId()).get().getCommcareSession());
        assertEquals(bytes, menuSessionService.getSessionById(session.getId()).getCommcareSession());
    }

    private Optional<SerializableMenuSession> getCachedSession(String sessionId) {
        return ofNullable(cacheManager.getCache("menu_session")).map(
                c -> c.get(sessionId, SerializableMenuSession.class)
        );
    }

    // only include the service under test and it's dependencies
    // This should not be necessary but we're using older versions of junit and spring
    @ComponentScan(
            basePackageClasses = {MenuSessionService.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                    MenuSessionService.class})
    )
    @EnableCaching
    @Configuration
    public static class Config {

        @MockBean
        public MenuSessionRepo menuSessionRepo;

        @MockBean
        public JdbcTemplate jdbcTemplate;
    }
}
