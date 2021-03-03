package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.FormSessionListViewRaw;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FormSessionRepo extends JpaRepository<SerializableFormSession, String> {
    List<FormSessionListView> findByUsernameAndDomainAndAsUserOrderByDateCreatedDesc(String username, String domain, String asUser, Pageable page);
    List<FormSessionListView> findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(String username, String domain, Pageable page);

    /**
     * @deprecated: to be removed once custom sorting is no longer required (once
     * the dateCreated field is fully populated)
     */
    @Deprecated
    @Query(
            value = "SELECT id, title, dateopened, datecreated, sessiondata " +
                    "FROM formplayer_sessions WHERE username = :username AND domain = :domain " +
                    "AND asuser = :asuser " +
                    "ORDER BY dateopened\\:\\:timestamptz DESC",
            nativeQuery = true
    )
    List<FormSessionListViewRaw> findUserSessionsAsUser(
            @Param("username") String username,
            @Param("domain") String domain,
            @Param("asuser") String asUser);

    /**
     * @deprecated: to be removed once custom sorting is no longer required (once
     * the dateCreated field is fully populated)
     */
    @Deprecated
    @Query(
            value = "SELECT id, title, dateopened, datecreated, sessiondata " +
                    "FROM formplayer_sessions WHERE username = :username AND domain = :domain " +
                    "AND asuser is null " +
                    "ORDER BY dateopened\\:\\:timestamptz DESC",
            nativeQuery = true
    )
    List<FormSessionListViewRaw> findUserSessionsNullAsUser(
            @Param("username") String username,
            @Param("domain") String domain);
/**
     * @deprecated: remove this once the ``version`` column is fully populated
     * @param id
     */
    @Deprecated
    @Transactional
    @Modifying
    @Query(value = "DELETE SerializableFormSession WHERE id = :id")
    void deleteSessionById(@Param("id") String id);
}
