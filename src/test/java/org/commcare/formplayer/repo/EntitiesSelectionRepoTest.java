package org.commcare.formplayer.repo;

import static org.assertj.core.api.Assertions.assertThat;

import org.commcare.formplayer.objects.EntitiesSelection;
import org.commcare.formplayer.utils.JpaTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class EntitiesSelectionRepoTest {

    @Autowired
    EntitiesSelectionRepo entitiesSelectionRepo;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testSaveAndLoad() {
        EntitiesSelection entitiesSelection = entitiesSelectionRepo.saveAndFlush(
                new EntitiesSelection(new String[]{"case1", "case3"}));
        assert entitiesSelection.getId() != null;

        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        EntitiesSelection loaded = JpaTestUtils.unwrapProxy(
                entitiesSelectionRepo.getById(entitiesSelection.getId())
        );
        assertThat(loaded).usingRecursiveComparison().isEqualTo(entitiesSelection);
    }
}

