package org.terracotta.utils;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Fabien Sanglier
 */
public class CacheUtils {
    public static final String ENV_CACHE_CONFIGPATH = "ehcache.config.path";
    private static Logger log = LoggerFactory.getLogger(CacheUtils.class);

    public static Cache getCache(String cacheName) throws Exception {
        return getCache(cacheName, null, null);
    }

    public static Cache getCache(String cacheName, String cacheManagerName) throws Exception {
        return getCache(cacheName, cacheManagerName, null);
    }

    public static Cache getCache(String cacheName, String cacheManagerName, String cacheMgrResourcePath) throws Exception {
        CacheManager cacheManager = getCacheManager(cacheManagerName, cacheMgrResourcePath);
        if (null == cacheName || !cacheManager.cacheExists(cacheName)) {
            log.info("EhCache xml not found or cache not found.");
            return null;
        }

        return cacheManager.getCache(cacheName);
    }

    public static CacheManager getCacheManager() {
        return getCacheManager(null);
    }

    public static CacheManager getCacheManager(String cacheManagerName) {
        return getCacheManager(cacheManagerName, null);
    }

    public static CacheManager getCacheManager(String cacheManagerName, String resourcePath) {
        CacheManager cm = null;

        if (null == (cm = CacheManager.getCacheManager(cacheManagerName))) {
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
                        inputStream = CacheUtils.class.getClassLoader().getResourceAsStream(configLocationToLoad.substring("classpath:".length()));
                    } else { //default to classpath if no prefix is specified
                        inputStream = CacheUtils.class.getClassLoader().getResourceAsStream(configLocationToLoad);
                    }

                    if (inputStream == null) {
                        throw new FileNotFoundException("File at '" + configLocationToLoad + "' not found");
                    }

                    log.info("Loading Cache manager from " + configLocationToLoad);
                    cm = CacheManager.create(inputStream);
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
                log.info("Loading Cache manager from default classpath");
                cm = CacheManager.getInstance();
            }
        }

        return cm;
    }
}
