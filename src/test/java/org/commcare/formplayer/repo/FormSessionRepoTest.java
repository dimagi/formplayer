package org.commcare.formplayer.repo;

import com.google.common.collect.ImmutableMap;
import org.commcare.formplayer.objects.FormSessionListDetailsView;
import org.commcare.formplayer.objects.FunctionHandler;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.utils.JpaTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class FormSessionRepoTest {

    @Autowired
    FormSessionRepo formSessionRepo;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testSaveAndLoad() {
        SerializableFormSession session = new SerializableFormSession();
        session.setInstanceXml("xml");
        session.setFormXml("form xml");
        session.setUsername("username");
        session.setSessionData(ImmutableMap.of("a", "1", "b",  "2"));
        session.setSequenceId(1);
        session.setInitLang("en");
        session.setDomain("domain");
        session.setPostUrl("/a/domain/receiver");
        session.setTitle("title");
        session.setDateOpened(new Date().toString());
        session.setOneQuestionPerScreen(true);
        session.setCurrentIndex("a0");
        session.setAsUser("asUser");
        session.setAppId("appId");
        FunctionHandler[] functionHandlers = {new FunctionHandler("count()", "123")};
        session.setFunctionContext(ImmutableMap.of("count", functionHandlers));
        session.setInPromptMode(false);
        session.setRestoreAsCaseId("restoreAsCaseId");

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

    @Test
    public void testGetListView() {
        ImmutableMap<String, String> sessionData = ImmutableMap.of("a", "1", "b", "2");
        String dateOpened = new Date().toString();

        SerializableFormSession session = new SerializableFormSession();
        session.setSequenceId(0);
        session.setUsername("momo");
        session.setDateOpened(dateOpened);
        session.setTitle("More Momo");
        session.setSessionData(sessionData);
        formSessionRepo.save(session);
        List<FormSessionListDetailsView> userSessions = formSessionRepo.findByUsername(
                "momo", Sort.by(Sort.Direction.DESC, "dateCreated")
        );
        assertThat(userSessions).hasSize(1);
        assertThat(userSessions.get(0).getTitle()).isEqualTo("More Momo");
        assertThat(userSessions.get(0).getDateOpened()).isEqualTo(dateOpened);
        assertThat(userSessions.get(0).getDateCreated()).isEqualTo(session.getDateCreated());
        assertThat(userSessions.get(0).getSessionData()).isEqualTo(sessionData);
        assertThat(userSessions.get(0).getId()).isEqualTo(session.getId());
    }
}
