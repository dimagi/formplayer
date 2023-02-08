package org.commcare.formplayer.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
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
public class MediaMetaDataServiceTest {

    @Autowired
    MediaMetaDataService mediaMetaDataService;
    MediaMetadataRecord mediaMetaData;
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

        when(mediaMetaDataRepo.findById(any())).thenAnswer(new Answer<Optional<MediaMetadataRecord>>() {
            @Override
            public Optional<MediaMetadataRecord> answer(InvocationOnMock invocation) throws Throwable {
                return Optional.ofNullable(mediaMetaData);
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
        testFile.delete();
    }

    @Test
    public void testSaveMetaData() {
        mediaMetaDataService.saveMediaMetaData(mediaMetaData);
        Optional<MediaMetadataRecord> fetchedMediaMetaData = mediaMetaDataRepo.findById(mediaMetaData.getId());
        Assertions.assertTrue(fetchedMediaMetaData.isPresent());
    }

    @Test
    public void testDelete() {
        mediaMetaDataService.saveMediaMetaData(mediaMetaData);
        Optional<MediaMetadataRecord> fetchedMediaMetaData = mediaMetaDataRepo.findById(mediaMetaData.getId());
        Assertions.assertTrue(fetchedMediaMetaData.isPresent());
        String metadataId = mediaMetaData.getId();
        mediaMetaDataService.deleteMetaDataById(metadataId);
        Optional<MediaMetadataRecord> newlyFetchedMediaMetaData = mediaMetaDataRepo.findById(metadataId);
        Assertions.assertFalse(newlyFetchedMediaMetaData.isPresent());
    }

    @Test
    public void testPurge() {
        Assertions.assertTrue(testFile.exists());
        Integer purgeCount = mediaMetaDataService.purge(Instant.now());
        Assertions.assertEquals(1, purgeCount);
        Assertions.assertFalse(testFile.exists());
    }

    @ComponentScan(basePackageClasses = {
            MediaMetaDataService.class}, useDefaultFilters = false, includeFilters = @ComponentScan.Filter(type
            = FilterType.ASSIGNABLE_TYPE, classes = {
            MediaMetaDataService.class}))
    @Configuration
    public static class Config {

        @MockBean
        public MediaMetaDataRepo mediaMetaDataRepo;

        @MockBean
        public JdbcTemplate jdbcTemplate;
    }
}
