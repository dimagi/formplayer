package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface FormSessionRepo extends JpaRepository<SerializableFormSession, String> {
    List<FormSessionListView> findByUsernameAndDomainAndAsUserOrderByDateCreatedDesc(String username, String domain, String asUser, Pageable page);
    long countByUsernameAndDomainAndAsUserOrderByDateCreatedDesc(String username, String domain, String asUser);
    List<FormSessionListView> findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(String username, String domain, Pageable page);
    long countByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(String username, String domain);

    @Modifying
    @Transactional
    @Query("DELETE from SerializableFormSession WHERE dateCreated < :cutoff")
    int deleteSessionsOlderThan(@Param("cutoff") Instant cutoff);
}
