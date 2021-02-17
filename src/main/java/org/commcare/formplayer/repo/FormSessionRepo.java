package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.SerializableFormSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FormSessionRepo extends JpaRepository<SerializableFormSession, String> {
    @Query(
        value = "SELECT * FROM formplayer_sessions WHERE username = :username " +
                "ORDER BY dateopened\\:\\:timestamptz DESC",
        nativeQuery = true
    )
    List<SerializableFormSession> findUserSessions(@Param("username") String username);

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
