package org.commcare.formplayer.services;

import static org.commcare.formplayer.util.Constants.VIRTUAL_DATA_INSTANCES_CACHE;

import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.formplayer.exceptions.InstanceNotFoundException;
import org.commcare.formplayer.objects.SerializableDataInstance;
import org.commcare.formplayer.repo.VirtualDataInstanceRepo;
import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeElement;
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
public class VirtualDataInstanceService implements VirtualDataInstanceStorage {

    @Autowired
    private VirtualDataInstanceRepo dataInstanceRepo;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    @Autowired
    CacheManager cacheManager;

    @Override
    public String write(ExternalDataInstance dataInstance) {
        SerializableDataInstance serializableDataInstance = new SerializableDataInstance(
                dataInstance.getInstanceId(), dataInstance.getReference(), storageFactory.getUsername(),
                storageFactory.getDomain(), storageFactory.getAppId(), storageFactory.getAsUsername(),
                (TreeElement)dataInstance.getRoot(), dataInstance.useCaseTemplate());
        SerializableDataInstance savedDataInstance = dataInstanceRepo.save(serializableDataInstance);
        Cache cache = cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE);
        cache.put(savedDataInstance.getId(), savedDataInstance);
        return savedDataInstance.getId();
    }

    @Override
    public ExternalDataInstance read(String key) {
        Cache cache = cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE);
        SerializableDataInstance serializableDataInstance = cache.get(key, SerializableDataInstance.class);
        if (serializableDataInstance == null) {
            Optional<SerializableDataInstance> optionalSerializableDataInstance = dataInstanceRepo.findById(key);
            if (optionalSerializableDataInstance.isPresent()) {
                serializableDataInstance = optionalSerializableDataInstance.get();
            }
        }
        if (serializableDataInstance != null
                && ifUsernameMatches(serializableDataInstance.getUsername(), storageFactory.getUsername())
                && serializableDataInstance.getAppId().equals(storageFactory.getAppId())) {
            ExternalDataInstanceSource instanceSource =
                    ExternalDataInstanceSource.buildVirtual(
                            serializableDataInstance.getInstanceId(), serializableDataInstance.getInstanceXml(),
                            serializableDataInstance.getReference(), serializableDataInstance.useCaseTemplate(), key);
            return instanceSource.toInstance();
        }
        throw new InstanceNotFoundException(key);
    }

    private boolean ifUsernameMatches(String username1, String username2) {
        return TableBuilder.scrubName(username1).contentEquals(TableBuilder.scrubName(username2));
    }

    public boolean contains(String key) {
        return dataInstanceRepo.existsById(key);
    }

    @CacheEvict(allEntries = true)
    public int purge(Instant cutoff) {
        return dataInstanceRepo.deleteSessionsOlderThan(cutoff);
    }
}
