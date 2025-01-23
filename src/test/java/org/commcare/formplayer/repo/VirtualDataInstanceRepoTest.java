package org.commcare.formplayer.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.commcare.formplayer.util.Constants.POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME;
import static org.javarosa.core.model.instance.ExternalDataInstance.JR_SELECTED_ENTITIES_REFERENCE;

import org.commcare.data.xml.SimpleNode;
import org.commcare.data.xml.TreeBuilder;
import org.commcare.formplayer.objects.SerializableDataInstance;
import org.commcare.formplayer.util.PrototypeUtils;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.persistence.EntityManager;

/**
 * Tests for {@link VirtualDataInstanceRepo}
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class VirtualDataInstanceRepoTest {

    @Autowired
    VirtualDataInstanceRepo virtualDataInstanceRepo;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        PrototypeUtils.setupThreadLocalPrototypes();
    }

    @Test
    public void testSaveAndLoad() {
        SerializableDataInstance savedInstance = virtualDataInstanceRepo.saveAndFlush(
                getSerializableDataInstance(new String[]{"case1", "case3"}, UUID.randomUUID().toString()));
        Assertions.assertNotNull(savedInstance.getId());
        Assertions.assertNotNull(savedInstance.getNamespacedKey());

        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        SerializableDataInstance loadedById = virtualDataInstanceRepo.getById(savedInstance.getId());
        Assertions.assertNotNull(loadedById);

        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        Assertions.assertTrue(virtualDataInstanceRepo.existsByNamespacedKey(savedInstance.getNamespacedKey()));

        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        Optional<SerializableDataInstance> loadedByKey = virtualDataInstanceRepo.findByNamespacedKey(savedInstance.getNamespacedKey());
        Assertions.assertTrue(loadedByKey.isPresent());
        Assertions.assertNotNull(loadedByKey.get().getDateCreated());
        assertInstanceXml(savedInstance, loadedByKey.get());
    }

    private void assertInstanceXml(SerializableDataInstance savedInstance, SerializableDataInstance loaded) {
        Assertions.assertNotNull(loaded);
        // Reattach parent
        ExternalDataInstance loadedExternalDataInstance = new ExternalDataInstance(
                JR_SELECTED_ENTITIES_REFERENCE,
                "selected_cases",
                loaded.getInstanceXml());
        assertThat(loadedExternalDataInstance.getRoot()).isEqualTo(savedInstance.getInstanceXml());
    }

    @Test
    public void testSaveAndLoad_manualKey() {
        String key = "123";
        SerializableDataInstance savedInstance = virtualDataInstanceRepo.saveAndFlush(
                getSerializableDataInstance(new String[]{"case1", "case3"}, key));
        Assertions.assertEquals(key, savedInstance.getNamespacedKey());

        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        Optional<SerializableDataInstance> loaded = virtualDataInstanceRepo.findByNamespacedKey(key);
        Assertions.assertTrue(loaded.isPresent());
    }

    @Test
    public void testDuplicateKey() {
        SerializableDataInstance savedInstance = virtualDataInstanceRepo.saveAndFlush(
                getSerializableDataInstance(new String[]{"case1", "case3"}, UUID.randomUUID().toString()));

        entityManager.clear(); // clear the EM cache to force a re-fetch from DB
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            virtualDataInstanceRepo.saveAndFlush(
                    getSerializableDataInstance(new String[]{"case1", "case3"}, savedInstance.getNamespacedKey()));
        });
    }

    @Test
    public void testDeleteByDateCreatedLessThan() {
        virtualDataInstanceRepo.deleteAll();
        Instant now = Instant.now();
        List<SerializableDataInstance> serializableDataInstances = IntStream.range(0, 5)
                .mapToObj(this::getSerializableDataInstance)
                .collect(Collectors.toList());
        List<SerializableDataInstance> savedInstances = virtualDataInstanceRepo.saveAll(serializableDataInstances);
        entityManager.flush();
        entityManager.clear();

        for (int i = 0; i < savedInstances.size(); i++) {
            SerializableDataInstance savedInstance = savedInstances.get(i);
            jdbcTemplate.update(
                    "UPDATE " + POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME + " SET datecreated = ? where id = ?",
                    Timestamp.from(now.minus(i, ChronoUnit.DAYS)), savedInstance.getId()
            );
        }
        List<String> allIds = jdbcTemplate.queryForList(
                "SELECT id FROM " + POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME,
                String.class);
        assertThat(allIds.size()).isEqualTo(5);

        int deleted = virtualDataInstanceRepo.deleteSessionsOlderThan(now.minus(2, ChronoUnit.DAYS));
        assertThat(deleted).isEqualTo(2);
        entityManager.flush();

        List<String> remainingIds = jdbcTemplate.queryForList(
                "SELECT id FROM " + POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME,
                String.class);
        assertThat(remainingIds.size()).isEqualTo(3);
    }

    private SerializableDataInstance getSerializableDataInstance(int i) {
        return getSerializableDataInstance(new String[]{"case" + i}, UUID.randomUUID().toString());
    }

    private SerializableDataInstance getSerializableDataInstance(String[] selections, String key) {
        ExternalDataInstance selectedEntitiesInstance = buildSelectedEntitiesInstance(selections);
        return new SerializableDataInstance(
                selectedEntitiesInstance.getInstanceId(),
                JR_SELECTED_ENTITIES_REFERENCE,
                "username",
                "domain",
                "appid",
                "asUser",
                (TreeElement)selectedEntitiesInstance.getRoot(),
                selectedEntitiesInstance.useCaseTemplate(),
                key);
    }

    public static ExternalDataInstance buildSelectedEntitiesInstance(String[] selections) {
        List<SimpleNode> nodes = new ArrayList<>();
        for (String selection : selections) {
            nodes.add(SimpleNode.textNode("value", Collections.emptyMap(), selection));
        }
        TreeElement root = TreeBuilder.buildTree("selected_cases", "results", nodes);
        return new ExternalDataInstance(JR_SELECTED_ENTITIES_REFERENCE, "selected_cases", root);
    }
}
