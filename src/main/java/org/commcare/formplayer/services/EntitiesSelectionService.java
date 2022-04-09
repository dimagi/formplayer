package org.commcare.formplayer.services;

import org.commcare.core.interfaces.EntitiesSelectionCache;
import org.commcare.formplayer.objects.EntitiesSelection;
import org.commcare.formplayer.repo.EntitiesSelectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

import javax.annotation.Nullable;

@Service
@CacheConfig(cacheNames = {"entities_selection"})
public class EntitiesSelectionService implements EntitiesSelectionCache {

    @Autowired
    private EntitiesSelectionRepo entitiesSelectionRepo;

    @Autowired
    CacheManager cacheManager;

    @Override
    public String cache(String[] values) {
        EntitiesSelection entitySelection = entitiesSelectionRepo.save(new EntitiesSelection(values));
        Cache cache = cacheManager.getCache("entities_selection");
        cache.put(entitySelection.getId(), entitySelection.getEntities());
        return entitySelection.getId();
    }

    @Nullable
    @Override
    @Cacheable
    public String[] read(String key) {
        Optional<EntitiesSelection> entitySelection = entitiesSelectionRepo.findById(key);
        if (entitySelection.isPresent()) {
            return entitySelection.get().getEntities();
        }
        return null;
    }

    @Override
    @Cacheable
    public boolean contains(String key) {
        return entitiesSelectionRepo.existsById(key);
    }
}
