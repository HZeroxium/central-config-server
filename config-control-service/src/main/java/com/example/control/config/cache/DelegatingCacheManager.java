package com.example.control.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe delegating cache manager that supports runtime provider switching.
 * <p>
 * This implementation wraps an actual CacheManager and delegates all operations to it.
 * The underlying manager can be swapped atomically via {@link #switchCacheManager(CacheManager)}
 * without affecting ongoing cache operations.
 * <p>
 * Key features:
 * <ul>
 *   <li>Thread-safe provider switching using AtomicReference</li>
 *   <li>Zero-downtime switching - operations continue on old manager until switch completes</li>
 *   <li>Delegation pattern - all CacheManager methods forwarded to current delegate</li>
 *   <li>Logging of provider switches for observability</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * // Create with initial manager
 * DelegatingCacheManager delegating = new DelegatingCacheManager(initialManager);
 * 
 * // Switch to new provider at runtime
 * delegating.switchCacheManager(newProviderManager);
 * }</pre>
 * 
 * @author Principal Software Engineer
 * @since 1.0.0
 */
@Slf4j
public class DelegatingCacheManager implements CacheManager {

    /**
     * Atomic reference to the current cache manager delegate.
     * All cache operations are delegated to this manager.
     */
    private final AtomicReference<CacheManager> delegateRef;

    /**
     * Creates a new delegating cache manager with the specified initial delegate.
     *
     * @param initialDelegate the initial cache manager to delegate to
     * @throws IllegalArgumentException if initialDelegate is null
     */
    public DelegatingCacheManager(CacheManager initialDelegate) {
        if (initialDelegate == null) {
            throw new IllegalArgumentException("Initial delegate cannot be null");
        }
        this.delegateRef = new AtomicReference<>(initialDelegate);
        log.info("Initialized DelegatingCacheManager with initial delegate: {}", 
            initialDelegate.getClass().getSimpleName());
    }

    /**
     * Atomically switches to a new cache manager.
     * <p>
     * This operation is thread-safe and atomic. All subsequent cache operations
     * will use the new manager immediately after this method returns.
     * <p>
     * The old manager is not automatically closed or cleaned up - this is the
     * responsibility of the caller if needed.
     *
     * @param newManager the new cache manager to delegate to
     * @throws IllegalArgumentException if newManager is null
     */
    public void switchCacheManager(CacheManager newManager) {
        if (newManager == null) {
            throw new IllegalArgumentException("New manager cannot be null");
        }
        
        CacheManager oldManager = delegateRef.getAndSet(newManager);
        
        log.info("Switched cache manager from {} to {}", 
            oldManager.getClass().getSimpleName(), 
            newManager.getClass().getSimpleName());
    }

    /**
     * Gets the current cache manager delegate.
     * <p>
     * This method is useful for debugging or introspection purposes.
     *
     * @return the current cache manager delegate
     */
    public CacheManager getCurrentDelegate() {
        return delegateRef.get();
    }

    /**
     * Gets the current delegate's class name for logging and monitoring.
     *
     * @return the simple class name of the current delegate
     */
    public String getCurrentDelegateType() {
        return delegateRef.get().getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the current cache manager.
     */
    @Override
    public Cache getCache(String name) {
        return delegateRef.get().getCache(name);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the current cache manager.
     */
    @Override
    public Collection<String> getCacheNames() {
        return delegateRef.get().getCacheNames();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the current cache manager.
     */
    @Override
    public String toString() {
        return String.format("DelegatingCacheManager{delegate=%s}", getCurrentDelegateType());
    }
}
