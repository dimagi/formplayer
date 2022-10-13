package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.SerializableDataInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * JpaRepository interface for {@link SerializableDataInstance}
 */
public interface VirtualDataInstanceRepo extends JpaRepository<SerializableDataInstance, String> {

    Optional<SerializableDataInstance> findByNamespacedKey(String key);

    boolean existsByNamespacedKey(String key);

    @Modifying
    @Transactional
    @Query("DELETE from SerializableDataInstance WHERE dateCreated < :cutoff")
    int deleteSessionsOlderThan(@Param("cutoff") Instant cutoff);
}
