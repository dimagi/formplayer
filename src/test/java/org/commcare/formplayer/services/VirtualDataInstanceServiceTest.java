package org.commcare.formplayer.services;

import static org.commcare.formplayer.repo.VirtualDataInstanceRepoTest.buildSelectedCasesInstance;
import static org.commcare.formplayer.util.Constants.VIRTUAL_DATA_INSTANCES_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static java.util.Optional.ofNullable;

import org.commcare.formplayer.objects.SerializableDataInstance;
import org.commcare.formplayer.repo.VirtualDataInstanceRepo;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.VirtualDataInstance;
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
                    SerializableDataInstance serializableDataInstance = (SerializableDataInstance)invocation.getArguments()[0];
                    ReflectionTestUtils.setField(serializableDataInstance, "id", UUID.randomUUID());
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
        VirtualDataInstance virtualDataInstance = buildSelectedCasesInstance(selectedValues);
        UUID recordId = virtualDataInstanceService.write(virtualDataInstance);

        // cache is populated on save
        assertEquals(virtualDataInstance.getRoot(), getCachedRecord(recordId).get().getInstanceXml());

        // get Record hits the cache (repo is mocked)
        ExternalDataInstance fetchedRecord = virtualDataInstanceService.read(recordId);
        assertEquals(virtualDataInstance.getRoot(), fetchedRecord.getRoot());
    }

    @Test
    public void testPurgeClearsCache() {
        String[] selectedValues = new String[]{"val1", "val2"};
        VirtualDataInstance virtualDataInstance = buildSelectedCasesInstance(selectedValues);
        UUID recordId = virtualDataInstanceService.write(virtualDataInstance);
        assertTrue(getCachedRecord(recordId).isPresent());
        virtualDataInstanceService.purge(Instant.now());
        assertFalse(getCachedRecord(recordId).isPresent());
    }

    private Optional<SerializableDataInstance> getCachedRecord(UUID recordId) {
        return ofNullable(cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE)).map(
                c -> c.get(recordId, SerializableDataInstance.class)
        );
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
