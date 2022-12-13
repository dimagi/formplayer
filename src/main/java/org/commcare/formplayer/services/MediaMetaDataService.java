package org.commcare.formplayer.services;

import org.commcare.formplayer.objects.MediaMetadataRecord;
import org.commcare.formplayer.repo.MediaMetaDataRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;


@Service
public class MediaMetaDataService {

    @Autowired
    private MediaMetaDataRepo mediaMetaDataRepo;

    @CacheEvict(allEntries = true)
    public int purge() {
        return mediaMetaDataRepo.deleteMetaDataWithoutFormSessionId();
    }

    private void saveMediaMetaData(MediaMetadataRecord mediaMetadataRecord) {
        MediaMetadataRecord savedMediaMetadataRecord = mediaMetaDataRepo.save(mediaMetadataRecord);
    }
}
