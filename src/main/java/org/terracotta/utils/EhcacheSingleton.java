package org.terracotta.utils;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by fabien.sanglier on 9/26/18.
 */
public class EhcacheSingleton {
    private static Logger LOG = LoggerFactory.getLogger(EhcacheSingleton.class);

    //the single instance
    public static EhcacheSingleton instance = new EhcacheSingleton();

    public static final String ENV_CACHE_CONFIGPATH = "ehcache.config.path";
    private static final String DEFAULT_CACHE_FILE_NAME = "classpath:ehcache4x.xml";

    private CacheManager cacheManager;

    // Empty constructor for singleton
    private EhcacheSingleton() {
        initializeCacheManager(DEFAULT_CACHE_FILE_NAME);
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public Ehcache getCache(String cacheName) {
        Ehcache ehCacheCache = null;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving cache for cacheName: " + cacheName);
        }

        // Get the cache if the cacheManager is not null
        if (cacheManager != null) {
            ehCacheCache = cacheManager.getEhcache(cacheName);
            if (ehCacheCache == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to retrieve cache from CacheManager.");
                }
            }
        } else { //The cache is disabled or there was a problem initializing the CacheManager
            if (LOG.isDebugEnabled()) {
                LOG.debug("CacheManager is null or caching is disabled, so cacheName " + cacheName + " not found.");
            }
        }

        return ehCacheCache;
    }

    private synchronized void initializeCacheManager(final String resourcePath) {
        String configLocationToLoad = null;
        if (null != resourcePath && !"".equals(resourcePath)) {
            configLocationToLoad = resourcePath;
        } else if (null != System.getProperty(ENV_CACHE_CONFIGPATH)) {
            configLocationToLoad = System.getProperty(ENV_CACHE_CONFIGPATH);
        }

        if (null != configLocationToLoad) {
            InputStream inputStream = null;
            try {
                if (configLocationToLoad.indexOf("file:") > -1) {
                    inputStream = new FileInputStream(configLocationToLoad.substring("file:".length()));
                } else if (configLocationToLoad.indexOf("classpath:") > -1) {
                    inputStream = EhcacheSingleton.class.getClassLoader().getResourceAsStream(configLocationToLoad.substring("classpath:".length()));
                } else { //default to classpath if no prefix is specified
                    inputStream = EhcacheSingleton.class.getClassLoader().getResourceAsStream(configLocationToLoad);
                }

                if (inputStream == null) {
                    throw new FileNotFoundException("File at '" + configLocationToLoad + "' not found");
                }

                LOG.info("Loading Cache manager from " + configLocationToLoad);
                cacheManager = CacheManager.create(inputStream);
            } catch (IOException ioe) {
                throw new CacheException(ioe);
            } finally {
                if (null != inputStream) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        throw new CacheException(e);
                    }
                    inputStream = null;
                }
            }
        } else {
            LOG.info("Loading Cache manager from default classpath");
            cacheManager = CacheManager.getInstance();
        }
    }
}
