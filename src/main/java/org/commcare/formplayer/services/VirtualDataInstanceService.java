package org.commcare.formplayer.services;

import static org.commcare.formplayer.util.Constants.VIRTUAL_DATA_INSTANCES_CACHE;

import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.formplayer.exceptions.InstanceNotFoundException;
import org.commcare.formplayer.objects.SerializableDataInstance;
import org.commcare.formplayer.repo.VirtualDataInstanceRepo;
import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.model.instance.ExternalDataInstance;
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

import javax.annotation.Nonnull;

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
        return write(UUID.randomUUID().toString(), dataInstance);
    }

    @Override
    public String write(String key, ExternalDataInstance dataInstance) {
        saveAndCacheInstance(getSerializableDataInstance(dataInstance, key));
        return key;
    }

    @Override
    public ExternalDataInstance read(String key, String instanceId) {
        String namespaceKey = namespaceKey(key);
        Cache cache = cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE);
        SerializableDataInstance savedInstance = cache.get(namespaceKey, SerializableDataInstance.class);
        if (savedInstance == null) {
            Optional<SerializableDataInstance> optionalSerializableDataInstance = dataInstanceRepo.findByNamespacedKey(namespaceKey);
            if (optionalSerializableDataInstance.isPresent()) {
                savedInstance = optionalSerializableDataInstance.get();
            }
        }
        if (validateInstance(savedInstance, key)) {
            return savedInstance.toInstance(instanceId, key);
        }
        throw new InstanceNotFoundException(key, getNamespace());
    }

    @Override
    public boolean contains(String key) {
        return dataInstanceRepo.existsByNamespacedKey(namespaceKey(key));
    }


    @CacheEvict(allEntries = true)
    public int purge(Instant cutoff) {
        return dataInstanceRepo.deleteSessionsOlderThan(cutoff);
    }

    private void saveAndCacheInstance(SerializableDataInstance serializableDataInstance) {
        SerializableDataInstance savedDataInstance = dataInstanceRepo.save(serializableDataInstance);
        Cache cache = cacheManager.getCache(VIRTUAL_DATA_INSTANCES_CACHE);
        cache.put(savedDataInstance.getNamespacedKey(), savedDataInstance);
    }

    @Nonnull
    private SerializableDataInstance getSerializableDataInstance(ExternalDataInstance dataInstance, String key) {
        String namespaceKey = namespaceKey(key);
        return new SerializableDataInstance(
                dataInstance.getInstanceId(), dataInstance.getReference(), storageFactory.getUsername(),
                storageFactory.getDomain(), storageFactory.getAppId(), storageFactory.getAsUsername(),
                (TreeElement)dataInstance.getRoot(), dataInstance.useCaseTemplate(), namespaceKey);
    }

    private boolean validateInstance(SerializableDataInstance savedInstance, String key) {
        if (savedInstance != null
                && ifUsernameMatches(savedInstance.getUsername(), storageFactory.getUsername())
                && savedInstance.getAppId().equals(storageFactory.getAppId())) {
            return true;
        }
        throw new InstanceNotFoundException(key, getNamespace());
    }

    private boolean ifUsernameMatches(String username1, String username2) {
        return TableBuilder.scrubName(username1).contentEquals(TableBuilder.scrubName(username2));
    }

    public String namespaceKey(String baseKey) {
        return String.format("%s:%s", getNamespace(), baseKey);
    }

    public String getNamespace() {
        String prefix = storageFactory.getDomain()
                + storageFactory.getAppId()
                + storageFactory.getUsername()
                + storageFactory.getAsUsername();
        return Integer.toString(prefix.hashCode());
    }
}
