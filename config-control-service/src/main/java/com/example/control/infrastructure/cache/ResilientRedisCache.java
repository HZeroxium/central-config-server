package com.example.control.infrastructure.cache;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.concurrent.Callable;

/**
 * Wrapper around {@link RedisCache} that handles deserialization errors gracefully.
 * <p>
 * When a {@link SerializationException} occurs during cache lookup (e.g., due to
 * corrupted or incompatible cached data), this wrapper:
 * <ol>
 * <li>Logs a warning with the cache key</li>
 * <li>Evicts the corrupted key from cache</li>
 * <li>Returns null (cache miss) to trigger fresh data fetch</li>
 * </ol>
 * This ensures that corrupted cache entries don't break the application and are
 * automatically cleaned up.
 * </p>
 */
@Slf4j
public class ResilientRedisCache implements Cache {

    private final RedisCache delegate;

    public ResilientRedisCache(RedisCache delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public Cache.ValueWrapper get(Object key) {
        try {
            return delegate.get(key);
        } catch (SerializationException e) {
            return handleDeserializationError(key, e);
        } catch (RuntimeException e) {
            // Check if it's a Jackson deserialization error wrapped in RuntimeException
            if (isDeserializationError(e)) {
                return handleDeserializationError(key, e);
            }
            // Re-throw other runtime exceptions
            throw e;
        } catch (Exception e) {
            // Handle any other unexpected exceptions
            log.error("Unexpected error retrieving cache entry for key: {} in cache: {}",
                    key, getName(), e);
            return null;
        }
    }
    
    /**
     * Check if the exception is related to deserialization errors.
     */
    private boolean isDeserializationError(Throwable e) {
        if (e == null) {
            return false;
        }
        // Check the exception itself
        if (e instanceof SerializationException) {
            return true;
        }
        if (e instanceof InvalidTypeIdException) {
            return true;
        }
        // Check the cause chain
        Throwable cause = e.getCause();
        while (cause != null && cause != e) {
            if (cause instanceof SerializationException || cause instanceof InvalidTypeIdException) {
                return true;
            }
            // Check for Jackson exceptions in the message
            String message = cause.getMessage();
            if (message != null && (message.contains("Could not resolve subtype") ||
                    message.contains("missing type id property") ||
                    message.contains("Could not read JSON"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
    
    /**
     * Handle deserialization errors by evicting the corrupted key and returning null.
     */
    private Cache.ValueWrapper handleDeserializationError(Object key, Throwable e) {
        log.warn("Failed to deserialize cache entry for key: {} in cache: {}. " +
                "Evicting corrupted key and treating as cache miss. Error: {}",
                key, getName(), e.getMessage());
        try {
            // Evict the corrupted key
            delegate.evict(key);
            log.debug("Evicted corrupted cache key: {} from cache: {}", key, getName());
        } catch (Exception evictException) {
            log.warn("Failed to evict corrupted cache key: {} from cache: {}",
                    key, getName(), evictException);
        }
        // Return null (cache miss) to trigger fresh data fetch
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        try {
            return delegate.get(key, type);
        } catch (SerializationException e) {
            handleDeserializationErrorForTypedGet(key, e);
            return null;
        } catch (RuntimeException e) {
            // Check if it's a Jackson deserialization error wrapped in RuntimeException
            if (isDeserializationError(e)) {
                handleDeserializationErrorForTypedGet(key, e);
                return null;
            }
            // Re-throw other runtime exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving cache entry for key: {} in cache: {}",
                    key, getName(), e);
            return null;
        }
    }
    
    /**
     * Handle deserialization errors for typed get methods.
     */
    private void handleDeserializationErrorForTypedGet(Object key, Throwable e) {
        log.warn("Failed to deserialize cache entry for key: {} in cache: {}. " +
                "Evicting corrupted key and treating as cache miss. Error: {}",
                key, getName(), e.getMessage());
        try {
            delegate.evict(key);
            log.debug("Evicted corrupted cache key: {} from cache: {}", key, getName());
        } catch (Exception evictException) {
            log.warn("Failed to evict corrupted cache key: {} from cache: {}",
                    key, getName(), evictException);
        }
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        try {
            return delegate.get(key, valueLoader);
        } catch (SerializationException e) {
            return handleDeserializationErrorWithReload(key, e, valueLoader);
        } catch (RuntimeException e) {
            // Check if it's a Jackson deserialization error wrapped in RuntimeException
            if (isDeserializationError(e)) {
                return handleDeserializationErrorWithReload(key, e, valueLoader);
            }
            // Re-throw other runtime exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in get(key, valueLoader) for key: {} in cache: {}",
                    key, getName(), e);
            // Try to reload the value
            try {
                return valueLoader.call();
            } catch (Exception loaderException) {
                log.error("Failed to reload value for key: {} in cache: {}",
                        key, getName(), loaderException);
                throw new RuntimeException("Failed to reload value after cache error", loaderException);
            }
        }
    }
    
    /**
     * Handle deserialization errors by evicting the corrupted key and reloading the value.
     */
    private <T> T handleDeserializationErrorWithReload(Object key, Throwable e, Callable<T> valueLoader) {
        log.warn("Failed to deserialize cache entry for key: {} in cache: {}. " +
                "Evicting corrupted key and reloading. Error: {}",
                key, getName(), e.getMessage());
        try {
            delegate.evict(key);
            log.debug("Evicted corrupted cache key: {} from cache: {}", key, getName());
        } catch (Exception evictException) {
            log.warn("Failed to evict corrupted cache key: {} from cache: {}",
                    key, getName(), evictException);
        }
        // Reload the value using the valueLoader
        try {
            return valueLoader.call();
        } catch (Exception loaderException) {
            log.error("Failed to reload value for key: {} in cache: {}",
                    key, getName(), loaderException);
            throw new RuntimeException("Failed to reload value after cache deserialization error", loaderException);
        }
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}

