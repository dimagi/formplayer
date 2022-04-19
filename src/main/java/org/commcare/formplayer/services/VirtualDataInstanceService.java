package org.commcare.formplayer.services;

import static org.commcare.formplayer.util.Constants.VIRTUAL_DATA_INSTANCES_CACHE;

import org.commcare.core.interfaces.VirtualDataInstanceCache;
import org.commcare.formplayer.exceptions.InstanceNotFoundException;
import org.commcare.formplayer.objects.SerializableDataInstance;
import org.commcare.formplayer.repo.VirtualDataInstanceRepo;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.VirtualDataInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@CacheConfig(cacheNames = {VIRTUAL_DATA_INSTANCES_CACHE})
public class VirtualDataInstanceService implements VirtualDataInstanceCache {

    @Autowired
    private VirtualDataInstanceRepo dataInstanceRepo;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    @Autowired
    CacheManager cacheManager;

    @Override
    public UUID write(VirtualDataInstance dataInstance) {
        SerializableDataInstance serializableDataInstance = new SerializableDataInstance(
                dataInstance.getInstanceId(), storageFactory.getUsername(),
                storageFactory.getDomain(), storageFactory.getAppId(), storageFactory.getAsUsername(),
                (TreeElement)dataInstance.getRoot());
        SerializableDataInstance savedDataInstance = dataInstanceRepo.save(serializableDataInstance);
        Cache cache = cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE);
        cache.put(savedDataInstance.getId(), savedDataInstance);
        return savedDataInstance.getId();
    }

    @Override
    public VirtualDataInstance read(UUID key) {
        Cache cache = cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE);
        SerializableDataInstance serializableDataInstance = cache.get(key, SerializableDataInstance.class);
        if(serializableDataInstance == null) {
            Optional<SerializableDataInstance> optionalSerializableDataInstance = dataInstanceRepo.findById(key);
            if (optionalSerializableDataInstance.isPresent()) {
                serializableDataInstance = optionalSerializableDataInstance.get();
            }
        }
        if (serializableDataInstance != null
                && serializableDataInstance.getUsername().equals(storageFactory.getUsername())
                && serializableDataInstance.getAppId().equals(storageFactory.getAppId())) {
            return new VirtualDataInstance(serializableDataInstance.getInstanceId(),
                    serializableDataInstance.getInstanceXml());
        }
        throw new InstanceNotFoundException(key);
    }

    public boolean contains(UUID key) {
        return dataInstanceRepo.existsById(key);
    }

    @CacheEvict(allEntries = true)
    public int purge(Instant cutoff) {
        return dataInstanceRepo.deleteSessionsOlderThan(cutoff);
    }
}
