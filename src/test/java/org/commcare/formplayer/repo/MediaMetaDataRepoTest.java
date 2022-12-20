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

    private SerializableFormSession session;


    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID().toString();
        session = new SerializableFormSession(sessionId);
        formSessionRepo.save(session);
    }


    @Test
    public void testSaveAndLoad() {

        MediaMetadataRecord mediaMetaData = new MediaMetadataRecord(
                "filePath",
                sessionId,
                "contentType",
                4,
                "username",
                "asUser",
                "domain",
                "appid"
        );

        mediaMetadataRepo.save(mediaMetaData);

        Assertions.assertNotNull(mediaMetaData);

        Assertions.assertNotNull(session);

        Assertions.assertEquals(session.getId(), mediaMetaData.getFormSessionId());
    }

}
