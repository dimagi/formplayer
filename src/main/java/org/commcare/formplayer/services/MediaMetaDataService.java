package org.commcare.formplayer.services;

import org.commcare.formplayer.exceptions.MediaMetaDataNotFoundException;
import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.commcare.formplayer.repo.MediaMetaDataRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void saveMediaMetaData(MediaMetadataRecord mediaMetadataRecord) {
        mediaMetaDataRepo.save(mediaMetadataRecord);
    }

    public void deleteMetaDataById(String id) {
        mediaMetaDataRepo.deleteById(id);
    }

    public List<MediaMetadataRecord> findAllWithNullFormsession() {
        return mediaMetaDataRepo.findAllFormSessionIsNull();
    }
}
