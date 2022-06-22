package org.commcare.formplayer.services;

import static org.commcare.formplayer.repo.VirtualDataInstanceRepoTest.buildSelectedEntitiesInstance;
import static org.commcare.formplayer.util.Constants.VIRTUAL_DATA_INSTANCES_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static java.util.Optional.ofNullable;

import org.commcare.formplayer.exceptions.InstanceNotFoundException;
import org.commcare.formplayer.objects.SerializableDataInstance;
import org.commcare.formplayer.repo.VirtualDataInstanceRepo;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
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

/**
 * Tests for {@link VirtualDataInstanceService}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class VirtualDataInstanceServiceTest {

    @Autowired
    VirtualDataInstanceService virtualDataInstanceService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private VirtualDataInstanceRepo virtualDataInstanceRepo;

    @Autowired
    FormplayerStorageFactory storageFactory;

    @BeforeEach
    public void setUp() {
        when(storageFactory.getUsername()).thenAnswer(invocation -> "username");
        when(storageFactory.getDomain()).thenAnswer(invocation -> "domain");
        when(storageFactory.getAppId()).thenAnswer(invocation -> "appId");
        when(storageFactory.getAsUsername()).thenAnswer(invocation -> "asUsername");
        // the repo always returns the saved object so simulate that in the mock
        when(virtualDataInstanceRepo.save(any())).thenAnswer(
                (Answer<SerializableDataInstance>)invocation -> {
                    SerializableDataInstance serializableDataInstance =
                            (SerializableDataInstance)invocation.getArguments()[0];
                    ReflectionTestUtils.setField(serializableDataInstance, "id", UUID.randomUUID().toString());
                    return serializableDataInstance;
                });
    }

    @AfterEach
    public void cleanup() {
        cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE).clear();
        storageFactory.getSQLiteDB().closeConnection();
    }

    @Test
    public void testGetRecordById_cached() {
        // save a Record
        String[] selectedValues = new String[]{"val1", "val2"};
        ExternalDataInstance externalDataInstance = buildSelectedEntitiesInstance(selectedValues);
        String recordId = virtualDataInstanceService.write(externalDataInstance);

        // cache is populated on save
        assertEquals(externalDataInstance.getRoot(), getCachedRecord(recordId).get().getInstanceXml());

        // get Record hits the cache (repo is mocked)
        ExternalDataInstance fetchedRecord = virtualDataInstanceService.read(recordId, "selected_cases");
        assertEquals(externalDataInstance.getRoot(), fetchedRecord.getRoot());
    }

    @Test
    public void testWriteWithManualKey() {
        // save a Record
        String[] selectedValues = new String[]{"val1", "val2"};
        ExternalDataInstance externalDataInstance = buildSelectedEntitiesInstance(selectedValues);
        String key = "123";
        String recordId = virtualDataInstanceService.write(key, externalDataInstance);

        assertEquals(key, recordId);

        // cache is populated on save
        assertEquals(externalDataInstance.getRoot(), getCachedRecord(recordId).get().getInstanceXml());

        // get Record hits the cache (repo is mocked)
        ExternalDataInstance fetchedRecord = virtualDataInstanceService.read(recordId, "selected_cases");
        assertEquals(externalDataInstance.getRoot(), fetchedRecord.getRoot());
    }

    @Test
    public void testReadByDifferentUser() {
        testReadByDifferentSessionDetail(() -> {
            when(storageFactory.getUsername()).thenAnswer(invocation -> "other user");
        });
    }

    @Test
    public void testReadByDifferentDomain() {
        testReadByDifferentSessionDetail(() -> {
            when(storageFactory.getDomain()).thenAnswer(invocation -> "other domain");
        });
    }

    @Test
    public void testReadByDifferentAppId() {
        testReadByDifferentSessionDetail(() -> {
            when(storageFactory.getAppId()).thenAnswer(invocation -> "other app");
        });
    }

    @Test
    public void testReadByDifferentAsUser() {
        testReadByDifferentSessionDetail(() -> {
            when(storageFactory.getAsUsername()).thenAnswer(invocation -> "other user");
        });
    }

    @Test
    public void testReadByNulls() {
        testReadByDifferentSessionDetail(() -> {
            when(storageFactory.getUsername()).thenAnswer(invocation -> null);
            when(storageFactory.getDomain()).thenAnswer(invocation -> null);
            when(storageFactory.getAppId()).thenAnswer(invocation -> null);
            when(storageFactory.getAsUsername()).thenAnswer(invocation -> null);
        });
    }

    public void testReadByDifferentSessionDetail(Runnable adjustSessionDetail) {
        // save a Record
        ExternalDataInstance externalDataInstance = buildSelectedEntitiesInstance(new String[]{"val1", "val2"});
        String key = "123";
        String recordId = virtualDataInstanceService.write(key, externalDataInstance);

        assertEquals(key, recordId);

        // get Record hits the cache (repo is mocked)
        ExternalDataInstance fetchedRecord = virtualDataInstanceService.read(recordId, "selected_cases");
        assertNotNull(fetchedRecord);

        // call the runnable to change the session detail in some way that should prevent reading the
        // instance with the same key
        adjustSessionDetail.run();

        assertThrows(InstanceNotFoundException.class, () -> {
            virtualDataInstanceService.read(recordId, "selected_cases");
        });
    }

    @Test
    public void testPurgeClearsCache() {
        String[] selectedValues = new String[]{"val1", "val2"};
        ExternalDataInstance externalDataInstance = buildSelectedEntitiesInstance(selectedValues);
        String recordId = virtualDataInstanceService.write(externalDataInstance);
        assertTrue(getCachedRecord(recordId).isPresent());
        virtualDataInstanceService.purge(Instant.now());
        assertFalse(getCachedRecord(recordId).isPresent());
    }

    private Optional<SerializableDataInstance> getCachedRecord(String recordId) {
        Cache cache = cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE);
        String namespaceKey = virtualDataInstanceService.namespaceKey(recordId);
        return ofNullable(cache.get(namespaceKey, SerializableDataInstance.class));
    }

    // only include the service under test and it's dependencies
    // This should not be necessary but we're using older versions of junit and spring
    @ComponentScan(
            basePackageClasses = {VirtualDataInstanceService.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                    VirtualDataInstanceService.class})
    )
    @EnableCaching
    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    public static class VirtualDataInstanceServiceTestConfig {

        @MockBean
        public VirtualDataInstanceRepo virtualDataInstanceRepo;

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
            return new ConcurrentMapCacheManager(VIRTUAL_DATA_INSTANCES_CACHE);
        }
    }
}
