package org.nethercutt.ullr.lucene;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;

import org.nethercutt.ullr.config.ConfigUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexProvider {
    private static final int DEFAULT_CACHE_MAX_SIZE = 32;
    private static final int DEFAULT_CACHE_REFRESH_SEC = 300;
    
    private LoadingCache<String, Indexer> indexerCache;

    public IndexProvider() {
        int cacheMaxSize = ConfigUtils.getIntProperty("cacheMaxSize", DEFAULT_CACHE_MAX_SIZE);
        int cacheRefreshSec = ConfigUtils.getIntProperty("cacheRefreshSec", DEFAULT_CACHE_REFRESH_SEC);

        log.info("IndexProvider cache size_max={}, refresh_sec={}", cacheMaxSize, cacheRefreshSec);
        indexerCache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterAccess(cacheRefreshSec, TimeUnit.SECONDS)
                .ticker(Ticker.systemTicker())
                .build(index -> new Indexer(index));
    }
    
    public Indexer getIndex(String index) {
        return indexerCache.get(index);
    }
}