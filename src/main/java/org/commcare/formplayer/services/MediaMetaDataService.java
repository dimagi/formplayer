package org.commcare.formplayer.services;

import static org.commcare.formplayer.services.MediaHandler.cleanMedia;

import org.commcare.formplayer.exceptions.MediaMetaDataNotFoundException;
import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.commcare.formplayer.repo.MediaMetaDataRepo;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MediaMetaDataService {

    @Autowired
    private MediaMetaDataRepo mediaMetaDataRepo;

    public MediaMetadataRecord findById(String id) {
        Optional<MediaMetadataRecord> record = mediaMetaDataRepo.findById(id);
        if (!record.isPresent()) {
            throw new MediaMetaDataNotFoundException(id);
        }
        return record.get();
    }

    public MediaMetadataRecord saveMediaMetaData(MediaMetadataRecord mediaMetadataRecord) {
        return mediaMetaDataRepo.save(mediaMetadataRecord);
    }

    public void deleteMetaDataById(String id) {
        mediaMetaDataRepo.deleteById(id);
    }

    public List<MediaMetadataRecord> findAllWithNullFormSession() {
        return mediaMetaDataRepo.getFormSessionIsNull();
    }

    public List<MediaMetadataRecord> findAll() {
        return mediaMetaDataRepo.findAll();
    }

    /**
     * Deletes obsolete media files and metadata
     */
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
