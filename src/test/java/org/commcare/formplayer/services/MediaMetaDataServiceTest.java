package org.commcare.formplayer.services;

import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.commcare.formplayer.repo.MediaMetaDataRepo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * Tests for {@link MediaMetaDataService}
 */

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
@ComponentScan(
        basePackageClasses = {MediaMetaDataService.class,},
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                MediaMetaDataService.class})
)
public class MediaMetaDataServiceTest {

    @Autowired
    MediaMetaDataService mediaMetaDataService;

    @Autowired
    MediaMetaDataRepo mediaMetaDataRepo;

    MediaMetadataRecord mediaMetaData;

    private String mediaId;

    @BeforeEach
    public void setUp() {
        mediaId = UUID.randomUUID().toString();
        mediaMetaData = new MediaMetadataRecord(
                mediaId,
                "filePath",
                null,
                "contentType",
                4,
                "username",
                "asUser",
                "domain",
                "appid"
        );
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

        mediaMetaDataService.deleteMetaDataById(mediaMetaData.getId());

        fetchedMediaMetaData = mediaMetaDataRepo.findById(mediaMetaData.getId());

        Assertions.assertFalse(fetchedMediaMetaData.isPresent());
    }

}
