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
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.annotation.Nullable;

@Service
@CacheConfig(cacheNames = {"entities_selection"})
public class EntitiesSelectionService implements EntitiesSelectionCache {

    private final Log log = LogFactory.getLog(EntitiesSelectionService.class);

    @Autowired
    private EntitiesSelectionRepo entitiesSelectionRepo;

    @Autowired(required = false)
    private StatsDClient datadogStatsDClient;

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
    public boolean contains(String key) {
        return entitiesSelectionRepo.existsById(key);
    }

    @CacheEvict(allEntries = true)
    public int purge() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        long start = System.currentTimeMillis();
        int deletedRows = entitiesSelectionRepo.deleteSessionsOlderThan(cutoff);
        long elapsed = System.currentTimeMillis() - start;
        log.info(String.format("Purged %d stale entities selections in %d ms", deletedRows, elapsed));
        if (datadogStatsDClient != null) {
            datadogStatsDClient.time("PostgresEntitiesSelectionRepo.purge.timeInMillis", elapsed);
        }
        return deletedRows;
    }
}
