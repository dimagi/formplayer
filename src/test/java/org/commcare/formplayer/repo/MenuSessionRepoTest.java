package org.commcare.formplayer.repo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.FormSessionListViewRaw;
import org.commcare.formplayer.objects.FunctionHandler;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.utils.JpaTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.SerializationUtils;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        assertThat(loaded.getVersion()).isEqualTo(0);

        formSessionRepo.saveAndFlush(loaded);
        assertThat(loaded.getDateCreated()).isEqualTo(dateCreated);
//        assertThat(loaded.getVersion()).isEqualTo(1);  Restore this once @Version annotation is added back
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
        String dateOpened = session.getDateOpened();
        Map<String, String> sessionData = session.getSessionData();
        formSessionRepo.save(session);
        List<FormSessionListView> userSessions = formSessionRepo.findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(
                "momo", "domain"
        );
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("More Momo");
        assertThat(userSessions.get(0).getDateOpened()).isEqualTo(dateOpened);
        assertThat(userSessions.get(0).getDateCreated()).isEqualTo(session.getDateCreated());
        assertThat(userSessions.get(0).getSessionData()).isEqualTo(sessionData);
        assertThat(userSessions.get(0).getId()).isEqualTo(session.getId());
    }

    @Test
    public void testGetListView_Ordering() {
        // create and save 3 sessions, reverse order of creation, extract IDs
        Iterator<String> sessionIdIterator = Stream.of(getSession(), getSession(), getSession()).map((session) -> {
            formSessionRepo.save(session);
            return session;
        }).map(SerializableFormSession::getId).collect(Collectors.toCollection(LinkedList::new))
                .descendingIterator();
        ArrayList<String> sessionIds = Lists.newArrayList(sessionIdIterator);

        List<FormSessionListView> userSessions = formSessionRepo.findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(
                "momo", "domain"
        );
        assertThat(userSessions).extracting("id").containsExactlyElementsOf(
                sessionIds
        );
    }

    @Test
    public void testGetListView_filterByDomain() {
        formSessionRepo.save(getSession("domain1", "session1"));
        formSessionRepo.save(getSession("domain2", "session2"));
        List<FormSessionListView> userSessions = formSessionRepo.findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(
                "momo", "domain1"
        );
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session1");

        userSessions = formSessionRepo.findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(
                "momo", "domain2"
        );
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session2");
    }

    @Test
    public void testGetListViewRaw() {
        SerializableFormSession session = getSession();
        String dateOpened = session.getDateOpened();
        Map<String, String> sessionData = session.getSessionData();
        formSessionRepo.save(session);
        List<FormSessionListViewRaw> userSessions = formSessionRepo.findUserSessionsNullAsUser("momo", "domain");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("More Momo");
        assertThat(userSessions.get(0).getDateOpened()).isEqualTo(dateOpened);
        assertThat(userSessions.get(0).getDateCreated()).isEqualTo(session.getDateCreated());
        Map<String, String> dbSessionData = (Map<String, String>) SerializationUtils.deserialize(userSessions.get(0).getSessionData());
        assertThat(dbSessionData).isEqualTo(sessionData);
        assertThat(userSessions.get(0).getId()).isEqualTo(session.getId());
    }

    @Test
    public void testGetListViewRaw_filterByDomain() {
        formSessionRepo.save(getSession("domain1", "session1"));
        formSessionRepo.save(getSession("domain2", "session2"));
        List<FormSessionListViewRaw> userSessions = formSessionRepo.findUserSessionsNullAsUser("momo", "domain1");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session1");

        userSessions = formSessionRepo.findUserSessionsNullAsUser("momo", "domain2");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session2");
    }

    @Test
    public void testGetListViewRaw_filterByAsUser() {
        formSessionRepo.save(getSession("domain1", "session_user1", "asUser1"));
        formSessionRepo.save(getSession("domain1", "session_user2", "asUser2"));
        formSessionRepo.save(getSession("domain1", "session_momo", null));
        List<FormSessionListViewRaw> userSessions = formSessionRepo.findUserSessionsAsUser("momo", "domain1", "asUser1");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session_user1");

        userSessions = formSessionRepo.findUserSessionsAsUser("momo", "domain1", "asUser2");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session_user2");

        userSessions = formSessionRepo.findUserSessionsNullAsUser("momo", "domain1");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session_momo");
    }

    @Test
    public void testGetListView_filterByAsUser() {
        formSessionRepo.save(getSession("domain1", "session_user1", "asUser1"));
        formSessionRepo.save(getSession("domain1", "session_user2", "asUser2"));
        formSessionRepo.save(getSession("domain1", "session_momo", null));
        List<FormSessionListView> userSessions = formSessionRepo.findByUsernameAndDomainAndAsUserOrderByDateCreatedDesc("momo", "domain1", "asUser1");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session_user1");

        userSessions = formSessionRepo.findByUsernameAndDomainAndAsUserOrderByDateCreatedDesc("momo", "domain1", "asUser2");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session_user2");

        userSessions = formSessionRepo.findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc("momo", "domain1");
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("session_momo");
    }

    @Test
    public void testUpdateableFields() {
        SerializableFormSession session = getSession();

        // save session
        formSessionRepo.saveAndFlush(session);
        int version = session.getVersion();

        session.incrementSequence();

        // update field that should not get updated in the DB
        ReflectionTestUtils.setField(session,"domain","newdomain");
        formSessionRepo.saveAndFlush(session);
        entityManager.refresh(session);

        // check that version is updated
        assertThat(session.getVersion()).isGreaterThan(version);
        assertThat(session.getDomain()).isEqualTo("domain");
    }

    private SerializableFormSession getSession() {
        return getSession("domain", "More Momo", null);
    }

    private SerializableFormSession getSession(String domain, String title) {
        return getSession(domain, title, null);
    }

    private SerializableFormSession getSession(String domain, String title, @Nullable String asUser) {
        FunctionHandler[] functionHandlers = {new FunctionHandler("count()", "123")};
        SerializableFormSession session = new SerializableFormSession(
                domain, "appId", "momo", asUser, "restoreAsCaseId",
                "/a/domain/receiver", null, title, true, "en", false,
                ImmutableMap.of("a", "1", "b", "2"),
                ImmutableMap.of("count", functionHandlers)
        );
        session.setInstanceXml("xml");
        session.setFormXml("form xml");
        session.incrementSequence();
        return session;
    }
}
