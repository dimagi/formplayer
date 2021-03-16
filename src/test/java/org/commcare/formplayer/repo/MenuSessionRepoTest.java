package org.commcare.formplayer.repo;


import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.utils.JpaTestUtils;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.test.utilities.MockApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.*;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class MenuSessionRepoTest {

    private MockApp mApp;
    private SessionWrapper sessionWrapper;

    @Autowired
    MenuSessionRepo menuSessionRepo;

    @Autowired
    private EntityManager entityManager;
    
    @BeforeEach
    public void setUp() throws Exception {
        mApp = new MockApp("/app_for_text_tests/");
        sessionWrapper = mApp.getSession();
    }

    @Test
    public void testSaveAndLoad() {
        SerializableMenuSession session = new SerializableMenuSession();
        session.setCommcareSession(serializeSession(sessionWrapper));
        session.setUsername("username");
        session.setDomain("domain");
        session.setAppId("appId");
        session.setInstallReference("archives/basic.ccz");
        session.setLocale("en");
        session.setAsUser("asUser");
        session.setOneQuestionPerScreen(true);
        session.setPreview(true);

        menuSessionRepo.saveAndFlush(session);
        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        SerializableMenuSession loaded = JpaTestUtils.unwrapProxy(
                menuSessionRepo.getOne(session.getId())
        );
        assertThat(loaded).usingRecursiveComparison().isEqualTo(session);
    }
}