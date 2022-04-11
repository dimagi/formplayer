package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.EntitiesSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * JpaRepository implementation for 'entities_selection' entity
 */
public interface EntitiesSelectionRepo extends JpaRepository<EntitiesSelection, String> {

    @Modifying
    @Transactional
    @Query("DELETE from EntitiesSelection WHERE dateCreated < :cutoff")
    int deleteSessionsOlderThan(@Param("cutoff") Instant cutoff);
}
