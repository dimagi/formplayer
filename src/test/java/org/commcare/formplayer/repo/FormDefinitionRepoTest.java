package org.commcare.formplayer.repo;

import java.time.Instant;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.commcare.formplayer.utils.JpaTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to ensure the FormDefinitionRepo behaves as expected
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class FormDefinitionRepoTest {

    @Autowired
    FormDefinitionRepo formDefinitionRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    public void setUp() {
        this.jdbcTemplate.execute("DELETE from formplayer_sessions");
        this.jdbcTemplate.execute("DELETE from form_definition");
    }

    @Test
    public void testSaveAndLoad() {
        SerializableFormDefinition formDef = new SerializableFormDefinition(
                "appId",
                "formXmlns",
                "formVersion",
                "formDef"
        );
        formDefinitionRepo.saveAndFlush(formDef);
        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        SerializableFormDefinition loaded = JpaTestUtils.unwrapProxy(
                formDefinitionRepo.getById(formDef.getId())
        );
        assertThat(loaded).usingRecursiveComparison().ignoringFields("dateCreated", "id").isEqualTo(formDef);
        Instant dateCreated = loaded.getDateCreated();
        assertThat(dateCreated).isNotNull();

        formDefinitionRepo.saveAndFlush(loaded);
        assertThat(loaded.getDateCreated()).isEqualTo(dateCreated);
    }

    @Test
    public void testFindByAppIdAndFormXmlnsAndFormVersion() {
        SerializableFormDefinition formDef = new SerializableFormDefinition(
                "appId",
                "formXmlns",
                "formVersion",
                "formDef"
        );
        formDefinitionRepo.save(formDef);
        Optional<SerializableFormDefinition> optFormDef = formDefinitionRepo.findByAppIdAndFormXmlnsAndFormVersion(
                "appId", "formXmlns", "formVersion"
        );
        assertThat(optFormDef.isPresent()).isTrue();
        SerializableFormDefinition fetchedFormDef = optFormDef.get();
        assertThat(fetchedFormDef.getAppId()).isEqualTo("appId");
        assertThat(fetchedFormDef.getFormXmlns()).isEqualTo("formXmlns");
        assertThat(fetchedFormDef.getFormVersion()).isEqualTo("formVersion");
        assertThat(fetchedFormDef.getDateCreated()).isEqualTo(formDef.getDateCreated());
    }
}
