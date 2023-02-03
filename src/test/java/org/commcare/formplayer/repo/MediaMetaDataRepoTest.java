package org.commcare.formplayer.repo;


import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.EntityManager;


/**
 * Tests for {@link MediaMetaDataRepo}
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class MediaMetaDataRepoTest {

    @Autowired
    public FormSessionRepo formSessionRepo;

    @Autowired
    public MediaMetaDataRepo mediaMetadataRepo;

    private String mediaId;

    private SerializableFormSession session;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        session = formSessionRepo.saveAndFlush(new SerializableFormSession());
        mediaId = UUID.randomUUID().toString();
    }

    @Test
    public void testSaveAndLoad() {
        MediaMetadataRecord mediaMetaData = new MediaMetadataRecord(
                mediaId,
                "filePath",
                session,
                "contentType",
                4,
                "username",
                "asUser",
                "domain",
                "appid"
        );
        MediaMetadataRecord savedMediaRecord = mediaMetadataRepo.saveAndFlush(mediaMetaData);
        Assertions.assertNotNull(session);
        Assertions.assertEquals(session.getId(), savedMediaRecord.getFormSession().getId());
    }

    @Test
    public void testFormSessionCascadeAsNull() {
        MediaMetadataRecord mediaMetaData = new MediaMetadataRecord(
                mediaId,
                "filePath",
                session,
                "contentType",
                4,
                "username",
                "asUser",
                "domain",
                "appid"
        );
        MediaMetadataRecord savedMediaRecord = mediaMetadataRepo.saveAndFlush(mediaMetaData);

        // delete form session
        formSessionRepo.delete(session);
        entityManager.flush();
        entityManager.clear();
        // get the meta data record to verify if the form session is set to null
        Assertions.assertNull(mediaMetadataRepo.findById(savedMediaRecord.getId()).get().getFormSession());
    }

    @Test
    public void testFindAllNullFormSession() {
        MediaMetadataRecord mediaMetaData = new MediaMetadataRecord(
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
        MediaMetadataRecord savedMediaRecord = mediaMetadataRepo.saveAndFlush(mediaMetaData);

        Optional<MediaMetadataRecord> fetchedMediaMetaData = mediaMetadataRepo.findById(mediaMetaData.getId());
        Assertions.assertTrue(fetchedMediaMetaData.isPresent());
        List<MediaMetadataRecord> records = mediaMetadataRepo.getFormSessionIsNull();
        Assertions.assertEquals(1, records.size());
    }
}
