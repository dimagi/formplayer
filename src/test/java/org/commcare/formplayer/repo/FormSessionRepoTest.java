package org.commcare.formplayer.repo;

import com.google.common.collect.ImmutableMap;
import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.FormSessionListViewRaw;
import org.commcare.formplayer.objects.FunctionHandler;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.utils.JpaTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class FormSessionRepoTest {

    @Autowired
    FormSessionRepo formSessionRepo;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testSaveAndLoad() {
        SerializableFormSession session = getSession();

        formSessionRepo.saveAndFlush(session);
        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        SerializableFormSession loaded = JpaTestUtils.unwrapProxy(
                formSessionRepo.getOne(session.getId())
        );
        assertThat(loaded).usingRecursiveComparison().ignoringFields("dateCreated", "version").isEqualTo(session);
        Instant dateCreated = loaded.getDateCreated();
        assertThat(dateCreated).isNotNull();
        assertThat(loaded.getVersion()).isEqualTo(1);

        formSessionRepo.saveAndFlush(loaded);
        assertThat(loaded.getDateCreated()).isEqualTo(dateCreated);
        assertThat(loaded.getVersion()).isEqualTo(2);
    }

    /**
     * Test that the session is deleted correctly even if ``version`` is null
     * as is the case with legacy data.
     */
    @Test
    public void testDeleteSession__nullVersion() {
        SerializableFormSession session = getSession();
        formSessionRepo.saveAndFlush(session);
        entityManager.clear();

        jdbcTemplate.update("update formplayer_sessions set version = null where id = ?", session.getId());
        formSessionRepo.deleteSessionById(session.getId());

        Optional<SerializableFormSession> byId = formSessionRepo.findById(session.getId());
        assertThat(byId).isEmpty();
    }

    @Test
    public void testGetListView() {
        SerializableFormSession session = getSession();
        Map<String, String> sessionData = session.getSessionData();
        String dateOpened = session.getDateOpened();
        formSessionRepo.save(session);
        List<FormSessionListView> userSessions = formSessionRepo.findByUsername(
                "momo", Sort.by(Sort.Direction.DESC, "dateCreated")
        );
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("More Momo");
        assertThat(userSessions.get(0).getDateOpened()).isEqualTo(dateOpened);
        assertThat(userSessions.get(0).getDateCreated()).isEqualTo(session.getDateCreated());
        assertThat(userSessions.get(0).getSessionData()).isEqualTo(sessionData);
        assertThat(userSessions.get(0).getId()).isEqualTo(session.getId());
    }

    @Test
    public void testGetListViewRaw() {
        SerializableFormSession session = getSession();
        Map<String, String> sessionData = session.getSessionData();
        String dateOpened = session.getDateOpened();
        formSessionRepo.save(session);
        List<FormSessionListViewRaw> userSessions = formSessionRepo.findUserSessions("momo");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("More Momo");
        assertThat(userSessions.get(0).getDateOpened()).isEqualTo(dateOpened);
        assertThat(userSessions.get(0).getDateCreated()).isEqualTo(session.getDateCreated());
        Map<String, String> dbSessionData = (Map<String, String>) SerializationUtils.deserialize(userSessions.get(0).getSessionData());
        assertThat(dbSessionData).isEqualTo(sessionData);
        assertThat(userSessions.get(0).getId()).isEqualTo(session.getId());
    }

    @Test
    public void testUpdateableFields() {
        SerializableFormSession session = getSession();

        // save session
        formSessionRepo.saveAndFlush(session);
        int version = session.getVersion();

        // update field that should not get updated in the DB
        ReflectionTestUtils.setField(session,"domain","newdomain");
        formSessionRepo.saveAndFlush(session);
        entityManager.refresh(session);

        // check that version is updated
        assertThat(session.getVersion()).isGreaterThan(version);
        assertThat(session.getDomain()).isEqualTo("domain");
    }

    private SerializableFormSession getSession() {
        FunctionHandler[] functionHandlers = {new FunctionHandler("count()", "123")};
        SerializableFormSession session = new SerializableFormSession(
                "domain", "appId", "momo", "asUser", "restoreAsCaseId",
                "/a/domain/receiver", null, "More Momo", true, "en", false,
                ImmutableMap.of("a", "1", "b", "2"),
                ImmutableMap.of("count", functionHandlers)
        );
        session.setInstanceXml("xml");
        session.setFormXml("form xml");
        session.incrementSequence();
        return session;
    }
}
