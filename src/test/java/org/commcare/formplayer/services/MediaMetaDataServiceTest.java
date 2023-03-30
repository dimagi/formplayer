package org.commcare.formplayer.services;

import static org.commcare.formplayer.util.Constants.MEDIA_METADATA_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import static java.util.Optional.ofNullable;

import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.exceptions.MediaMetaDataNotFoundException;
import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.commcare.formplayer.repo.MediaMetaDataRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tests for {@link MediaMetaDataService}
 */

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@EnableConfigurationProperties(value = CacheConfiguration.class)
@TestPropertySource("classpath:application.properties")
public class MediaMetaDataServiceTest {

    @Autowired
    MediaMetaDataService mediaMetaDataService;
    MediaMetadataRecord mediaMetaData;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    private MediaMetaDataRepo mediaMetaDataRepo;
    private String fileId;
    private File testFile;
    private String testFilePath;

    @BeforeEach
    public void setUp() throws IOException {
        fileId = UUID.randomUUID().toString();
        testFilePath = "src/test/resources/media/" + fileId + ".jpg";
        testFile = new File(testFilePath);
        testFile.createNewFile();
        mediaMetaData = new MediaMetadataRecord(
                fileId,
                testFilePath,
                null,
                "jpg",
                4,
                "username",
                "asUser",
                "domain",
                "appid"
        );

        when(mediaMetaDataRepo.save(any())).thenAnswer(new Answer<MediaMetadataRecord>() {
            @Override
            public MediaMetadataRecord answer(InvocationOnMock invocation) throws Throwable {
                MediaMetadataRecord mediaMetaData = (MediaMetadataRecord)invocation.getArguments()[0];
                if (mediaMetaData.getId() == null) {
                    // this is normally taken care of by Hibernate
                    ReflectionTestUtils.setField(mediaMetaData, "id", UUID.randomUUID().toString());
                }
                return mediaMetaData;
            }
        });

        doAnswer(invocation -> {
            mediaMetaData = null;
            return null;
        }).when(mediaMetaDataRepo).deleteById(any());

        when(mediaMetaDataRepo.findByFormSessionIsNull()).thenAnswer(new Answer<List<MediaMetadataRecord>>() {
            @Override
            public List<MediaMetadataRecord> answer(InvocationOnMock invocation) throws Throwable {
                List<MediaMetadataRecord> recordsList = new ArrayList<>();
                recordsList.add(mediaMetaData);
                return recordsList;
            }
        });
    }

    @AfterEach
    public void tearDown() {
        cacheManager.getCache(MEDIA_METADATA_CACHE).clear();
        testFile.delete();
    }

    @Test
    public void testMediaMetadataCacheExists() {
        Cache cache = cacheManager.getCache(MEDIA_METADATA_CACHE);
        assertNotNull(cache);
        assertTrue(cache instanceof CaffeineCache);
    }

    @Test
    public void testSaveMetaData_cached() {
        cacheManager.getCache(MEDIA_METADATA_CACHE).clear();

        mediaMetaDataService.saveMediaMetaData(mediaMetaData);

        assertEquals(mediaMetaData, getCachedMetadata(mediaMetaData.getFileId()).get());

        MediaMetadataRecord fetchedMediaMetaData = mediaMetaDataService.findByFileId(mediaMetaData.getFileId());
        assertEquals(mediaMetaData, fetchedMediaMetaData);
    }

    @Test
    public void testDelete() {
        mediaMetaDataService.saveMediaMetaData(mediaMetaData);
        String fileId = mediaMetaData.getFileId();
        MediaMetadataRecord fetchedMediaMetaData = mediaMetaDataService.findByFileId(fileId);
        Assertions.assertNotNull(fetchedMediaMetaData);
        mediaMetaDataService.deleteByFileId(fileId);
        Assertions.assertThrows(MediaMetaDataNotFoundException.class, () -> {
            mediaMetaDataService.findByFileId(fileId);
        });
    }

    @Test
    public void testPurge() {
        mediaMetaDataService.saveMediaMetaData(mediaMetaData);
        assertEquals(mediaMetaData, getCachedMetadata(mediaMetaData.getFileId()).get());
        String fileId = mediaMetaData.getFileId();

        Assertions.assertTrue(testFile.exists());
        Integer purgeCount = mediaMetaDataService.purge(Instant.now());
        Assertions.assertEquals(1, purgeCount);
        Assertions.assertFalse(testFile.exists());

        Assertions.assertEquals(Optional.empty(), getCachedMetadata(fileId));
    }

    private Optional<MediaMetadataRecord> getCachedMetadata(String fileId) {
        return ofNullable(cacheManager.getCache(MEDIA_METADATA_CACHE)).map(
                c -> c.get(fileId, MediaMetadataRecord.class)
        );
    }

    @ComponentScan(basePackageClasses = {
            MediaMetaDataService.class}, useDefaultFilters = false, includeFilters = @ComponentScan.Filter(type
            = FilterType.ASSIGNABLE_TYPE, classes = {
            MediaMetaDataService.class}))
    @EnableCaching
    @Configuration
    public static class Config {

        @MockBean
        public MediaMetaDataRepo mediaMetaDataRepo;

        @MockBean
        public JdbcTemplate jdbcTemplate;
    }
}
