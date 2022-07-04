package org.commcare.formplayer.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties(prefix = "caching")
@Data
@Slf4j
public class CacheConfiguration {

    @Data
    public static class CacheSpec {
        private Integer expireAfterWrite;
        private Integer expireAfterAccess;
        private Integer maximumSize = 500;
    }

    private Map<String, CacheSpec> specs;

    @Bean
    public CacheManager cacheManager(Ticker ticker) {
        SimpleCacheManager manager = new SimpleCacheManager();
        if (specs != null) {
            List<CaffeineCache> caches =
                    specs.entrySet().stream()
                            .map(entry -> buildCache(entry.getKey(), entry.getValue(), ticker))
                            .collect(Collectors.toList());
            manager.setCaches(caches);
        }
        return manager;
    }

    private CaffeineCache buildCache(String name, CacheSpec cacheSpec, Ticker ticker) {
        log.info("Cache '{}' configured - expireAfterWrite: {}, expireAfterAccess: {}, max size: {}",
                name, cacheSpec.getExpireAfterWrite(), cacheSpec.getExpireAfterAccess(),
                cacheSpec.getMaximumSize());
        final Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .expireAfterWrite(cacheSpec.getExpireAfterWrite(), TimeUnit.SECONDS)
                .expireAfterAccess(cacheSpec.getExpireAfterAccess(), TimeUnit.SECONDS)
                .maximumSize(cacheSpec.getMaximumSize())
                .ticker(ticker);
        return new CaffeineCache(name, caffeineBuilder.build());
    }

    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

}
