package org.commcare.formplayer.repo;

import static org.assertj.core.api.Assertions.assertThat;

import org.commcare.formplayer.objects.EntitiesSelection;
import org.commcare.formplayer.utils.JpaTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class EntitiesSelectionRepoTest {

    @Autowired
    EntitiesSelectionRepo entitiesSelectionRepo;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testSaveAndLoad() {
        EntitiesSelection entitiesSelection = entitiesSelectionRepo.saveAndFlush(
                getEntitiesSelection(new String[]{"case1", "case3"}));
        Assertions.assertNotNull(entitiesSelection.getId());

        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        EntitiesSelection loaded = JpaTestUtils.unwrapProxy(
                entitiesSelectionRepo.getById(entitiesSelection.getId())
        );
        assertThat(loaded).usingRecursiveComparison().ignoringFields("dateCreated").isEqualTo(entitiesSelection);
        Assertions.assertNotNull(loaded.getDateCreated());
    }

    @Test
    public void testDeleteByDateCreatedLessThan() {
        Instant now = Instant.now();

        List<EntitiesSelection> entitiesSelections = IntStream.range(0, 5)
                .mapToObj(this::getEntitiySelection)
                .collect(Collectors.toList());
        List<EntitiesSelection> savedEntitiesSelections = entitiesSelectionRepo.saveAll(entitiesSelections);
        entityManager.flush();
        entityManager.clear();

        for (int i = 0; i < savedEntitiesSelections.size(); i++) {
            EntitiesSelection entitiesSelection = savedEntitiesSelections.get(i);
            jdbcTemplate.update(
                    "UPDATE entities_selection SET datecreated = ? where id = ?",
                    Timestamp.from(now.minus(i, ChronoUnit.DAYS)), entitiesSelection.getId()
            );
        }
        List<String> allIds = jdbcTemplate.queryForList("SELECT id FROM entities_selection",
                String.class);
        assertThat(allIds.size()).isEqualTo(5);

        int deleted = entitiesSelectionRepo.deleteSessionsOlderThan(now.minus(2, ChronoUnit.DAYS));
        assertThat(deleted).isEqualTo(2);
        entityManager.flush();

        List<String> remainingIds = jdbcTemplate.queryForList("SELECT id FROM entities_selection",
                String.class);
        assertThat(remainingIds.size()).isEqualTo(3);
    }

    private EntitiesSelection getEntitiySelection(int i) {
        return getEntitiesSelection(new String[]{"case" + i});
    }

    private EntitiesSelection getEntitiesSelection(String[] selections) {
        return new EntitiesSelection("username", "domain", "appid", "asUser", selections);
    }
}

