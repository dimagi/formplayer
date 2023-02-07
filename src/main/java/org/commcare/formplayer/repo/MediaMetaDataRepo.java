package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * JpaRepository interface for {@link MediaMetadataRecord}
 */
public interface MediaMetaDataRepo extends JpaRepository<MediaMetadataRecord, String> {

    List<MediaMetadataRecord> findByFormSession(String formSessionId);
    
    List<MediaMetadataRecord> findByFormSessionIsNull();
}
