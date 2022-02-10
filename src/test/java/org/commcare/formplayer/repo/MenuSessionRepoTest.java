package org.commcare.formplayer.repo;


import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.util.serializer.SessionSerializer;
import org.commcare.formplayer.utils.JpaTestUtils;
import org.commcare.session.CommCareSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class MenuSessionRepoTest {

    private CommCareSession sessionWrapper;

    @Autowired
    MenuSessionRepo menuSessionRepo;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    public void setUp() throws Exception {
        sessionWrapper = new CommCareSession();
    }

    @Test
    public void testSaveAndLoad() {
        SerializableMenuSession session = new SerializableMenuSession(
                "username",
                "domain",
                "appId",
                "archives/basic.ccz",
                "en",
                "asUser",
                true
        );
        session.setCommcareSession(SessionSerializer.serialize(sessionWrapper));
        menuSessionRepo.saveAndFlush(session);
        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        SerializableMenuSession loaded = JpaTestUtils.unwrapProxy(
                menuSessionRepo.getById(session.getId())
        );
        assertThat(loaded).usingRecursiveComparison().isEqualTo(session);
    }
}
