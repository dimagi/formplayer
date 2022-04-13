package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.interfaces.EntitiesSelectionCache;
import org.commcare.formplayer.objects.EntitiesSelection;
import org.commcare.formplayer.repo.EntitiesSelectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

@Service
@CacheConfig(cacheNames = {"entities_selection"})
public class EntitiesSelectionService implements EntitiesSelectionCache {

    private final Log log = LogFactory.getLog(EntitiesSelectionService.class);

    @Autowired
    private EntitiesSelectionRepo entitiesSelectionRepo;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    @Autowired(required = false)
    private StatsDClient datadogStatsDClient;

    @Autowired
    CacheManager cacheManager;

    @Override
    public UUID write(String[] values) {
        EntitiesSelection entitySelection =  new EntitiesSelection(storageFactory.getUsername(),
                storageFactory.getDomain(), storageFactory.getAppId(), storageFactory.getAsUsername(), values);
        EntitiesSelection entitiesSelection = entitiesSelectionRepo.save(entitySelection);
        Cache cache = cacheManager.getCache("entities_selection");
        cache.put(entitiesSelection.getId(), entitiesSelection.getEntities());
        return entitiesSelection.getId();
    }

    @Nullable
    @Override
    @Cacheable
    public String[] read(UUID key) {
        Optional<EntitiesSelection> entitySelectionOptional = entitiesSelectionRepo.findById(key);
        if (entitySelectionOptional.isPresent()) {
            EntitiesSelection entitiesSelection = entitySelectionOptional.get();
            if(entitiesSelection.getUsername().equals(storageFactory.getUsername())
                    && entitiesSelection.getAppId().equals(storageFactory.getAppId())) {
                return entitiesSelection.getEntities();
            }
        }
        return null;
    }

    @Override
    public boolean contains(UUID key) {
        return entitiesSelectionRepo.existsById(key);
    }

    @CacheEvict(allEntries = true)
    public int purge(Instant cutoff) {
        return entitiesSelectionRepo.deleteSessionsOlderThan(cutoff);
    }
}
