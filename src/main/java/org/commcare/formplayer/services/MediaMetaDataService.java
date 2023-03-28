package org.commcare.formplayer.services;

import static org.commcare.formplayer.services.MediaHandler.cleanMedia;
import static org.commcare.formplayer.util.Constants.MEDIA_METADATA_CACHE;

import org.commcare.formplayer.exceptions.MediaMetaDataNotFoundException;
import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.commcare.formplayer.repo.MediaMetaDataRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


/**
 * Service for managing media metadata
 */
@Service
@CacheConfig(cacheNames = {MEDIA_METADATA_CACHE})
public class MediaMetaDataService {

    @Autowired
    private MediaMetaDataRepo mediaMetaDataRepo;

    @Cacheable
    public MediaMetadataRecord findByFileId(String id) {
        Optional<MediaMetadataRecord> record = mediaMetaDataRepo.findByFileId(id);
        if (!record.isPresent()) {
            throw new MediaMetaDataNotFoundException(id);
        }
        return record.get();
    }

    @CachePut(key = "#mediaMetadataRecord.id")
    public MediaMetadataRecord saveMediaMetaData(MediaMetadataRecord mediaMetadataRecord) {
        return mediaMetaDataRepo.save(mediaMetadataRecord);
    }

    public void deleteMetaDataById(String id) {
        mediaMetaDataRepo.deleteById(id);
    }

    public List<MediaMetadataRecord> findAllWithNullFormSession() {
        return mediaMetaDataRepo.findByFormSessionIsNull();
    }

    /**
     * Deletes obsolete media files and metadata
     */
    @CacheEvict(allEntries = true)
    public Integer purge(Instant instant) {
        List<MediaMetadataRecord> metadataToDelete = findAllWithNullFormSession();
        Integer deletedCount = 0;
        for (int i = 0; i < metadataToDelete.size(); i++) {
            MediaMetadataRecord metadata = metadataToDelete.get(i);
            Path parentPath = Paths.get(metadata.getFilePath()).getParent();
            String fileIdWithExt = metadata.getFileId() + "." + metadata.getContentType();
            Boolean deletedSuccessfully = cleanMedia(parentPath, fileIdWithExt, this);
            if (deletedSuccessfully) {
                deletedCount++;
            }
        }
        return deletedCount;
    }
}
