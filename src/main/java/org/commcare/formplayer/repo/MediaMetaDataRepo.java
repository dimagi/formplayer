package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JpaRepository interface for {@link MediaMetadataRecord}
 */
public interface MediaMetaDataRepo extends JpaRepository<MediaMetadataRecord, String> {

    Optional<MediaMetadataRecord> findByFormSessionId(String formSessionId);


    @Modifying
    @Transactional
    @Query("DELETE FROM MediaMetadataRecord WHERE formSession IS NULL")
    int deleteMetaDataWithoutFormSessionId();
}
