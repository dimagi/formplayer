package org.commcare.formplayer.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static java.util.Optional.ofNullable;

import org.commcare.formplayer.objects.EntitiesSelection;
import org.commcare.formplayer.repo.EntitiesSelectionRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class EntitiesSelectionServiceTest {

    @Autowired
    EntitiesSelectionService entitiesSelectionService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private EntitiesSelectionRepo entitiesSelectionRepo;

    @Autowired
    FormplayerStorageFactory storageFactory;

    @BeforeEach
    public void setUp() {
        when(storageFactory.getUsername()).thenAnswer(invocation -> "username");
        when(storageFactory.getDomain()).thenAnswer(invocation -> "domain");
        when(storageFactory.getAppId()).thenAnswer(invocation -> "appId");
        when(storageFactory.getAsUsername()).thenAnswer(invocation -> "asUsername");
        // the repo always returns the saved object so simulate that in the mock
        when(entitiesSelectionRepo.save(any())).thenAnswer(
                (Answer<EntitiesSelection>)invocation -> {
                    EntitiesSelection entitiesSelection = (EntitiesSelection)invocation.getArguments()[0];
                    ReflectionTestUtils.setField(entitiesSelection, "id", UUID.randomUUID());
                    return entitiesSelection;
                });
    }

    @AfterEach
    public void cleanup() {
        cacheManager.getCache("entities_selection").clear();
        storageFactory.getSQLiteDB().closeConnection();
    }

    @Test
    public void testGetRecordById_cached() {
        // save a Record
        String[] selectedValues = new String[]{"val1", "val2"};
        UUID recordId = entitiesSelectionService.write(selectedValues);

        // cache is populated on save
        assertEquals(selectedValues, getCachedRecord(recordId).get());

        // get Record hits the cache (repo is mocked)
        String[] fetchedRecord = entitiesSelectionService.read(recordId);
        assertEquals(selectedValues, fetchedRecord);
    }

    @Test
    public void testPurgeClearsCache() {
        String[] selectedValues = new String[]{"val1", "val2"};
        UUID recordId = entitiesSelectionService.write(selectedValues);
        assertTrue(getCachedRecord(recordId).isPresent());
        entitiesSelectionService.purge(Instant.now());
        assertFalse(getCachedRecord(recordId).isPresent());
    }

    private Optional<String[]> getCachedRecord(UUID recordId) {
        return ofNullable(cacheManager.getCache("entities_selection")).map(
                c -> c.get(recordId, String[].class)
        );
    }

    // only include the service under test and it's dependencies
    // This should not be necessary but we're using older versions of junit and spring
    @ComponentScan(
            basePackageClasses = {EntitiesSelectionService.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                    EntitiesSelectionService.class})
    )
    @EnableCaching
    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    public static class EntitiesSelectionServiceTestConfig {

        @MockBean
        public EntitiesSelectionRepo entitiesSelectionRepo;

        @MockBean
        public JdbcTemplate jdbcTemplate;

        @MockBean
        public FormSessionService formSessionService;

        @Bean
        public FormplayerStorageFactory storageFactory() {
            return Mockito.spy(FormplayerStorageFactory.class);
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("entities_selection");
        }
    }
}
