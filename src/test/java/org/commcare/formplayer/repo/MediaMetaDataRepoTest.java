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

import java.util.UUID;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class MediaMetaDataRepoTest {

    @Autowired
    public FormSessionRepo formSessionRepo;

    @Autowired
    public MediaMetaDataRepo mediaMetadataRepo;
    

    private String sessionId;

    private String mediaId;

    private SerializableFormSession session;


    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID().toString();
        mediaId = UUID.randomUUID().toString();
        session = new SerializableFormSession(sessionId);
        formSessionRepo.save(session);
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

        Assertions.assertNotNull(mediaMetaData);

        mediaMetadataRepo.save(mediaMetaData);

        Assertions.assertNotNull(session);

        Assertions.assertEquals(session.getId(), mediaMetaData.getFormSession().getId());
    }

}
