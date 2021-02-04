package org.commcare.formplayer.repo;

import com.google.common.collect.ImmutableMap;
import org.commcare.formplayer.objects.FunctionHandler;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
        SerializableFormSession loaded = formSessionRepo.getOne(session.getId());
        assertThat(loaded).usingRecursiveComparison().ignoringAllOverriddenEquals().isEqualTo(session);
    }
}
