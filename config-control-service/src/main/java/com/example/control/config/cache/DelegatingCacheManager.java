package com.example.control.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread-safe, delegating {@link CacheManager} that allows <em>runtime switching</em>
 * of the underlying cache provider without service downtime.
 *
 * <p><strong>What it does</strong></p>
 * <ul>
 *   <li>Wraps a concrete {@link CacheManager} (the <em>delegate</em>) in an {@link AtomicReference}
 *       so that the reference can be swapped atomically.</li>
 *   <li>Forwards all {@link CacheManager} operations ({@link #getCache(String)}, {@link #getCacheNames()})
 *       to the <em>current</em> delegate at the moment of invocation.</li>
 *   <li>Supports zero-downtime switching via {@link #switchCacheManager(CacheManager)}; in-flight calls
 *       continue on the old delegate, and subsequent calls observe the new one immediately.</li>
 *   <li>Emits logs for provider changes to improve observability.</li>
 * </ul>
 *
 * <p><strong>Thread-safety & visibility</strong></p>
 * <ul>
 *   <li>All reads/writes to the delegate are performed through {@link AtomicReference}, which provides
 *       atomic, lock-free updates with proper memory visibility guarantees (volatile semantics).</li>
 *   <li>No global synchronization is performed around cache operations; each call obtains a snapshot of
 *       the current delegate and proceeds, which is appropriate for a <em>switchable</em> component.</li>
 * </ul>
 *
 * <p><strong>Usage</strong></p>
 * <pre>{@code
 * // Create with initial provider (e.g., Caffeine or Redis)
 * CacheManager initial = ...
 * DelegatingCacheManager delegating = new DelegatingCacheManager(initial);
 *
 * // Later at runtime: switch to another provider atomically
 * CacheManager newProvider = ...
 * delegating.switchCacheManager(newProvider);
 * }</pre>
 *
 * <p><strong>Notes & limitations</strong></p>
 * <ul>
 *   <li>This class does not attempt to reconcile state between old/new providers. If you need warmup,
 *       promotion, or key migration, orchestrate that externally before/after switching.</li>
 *   <li>The old delegate is not closed/cleaned automatically. If it implements a lifecycle (e.g., {@code Closeable}
 *       or Spring {@code SmartLifecycle}), the caller is responsible for orderly shutdown and resource cleanup.</li>
 *   <li>The set of cache names is defined by the underlying delegate(s). If different providers expose different
 *       names, callers should expect that {@link #getCacheNames()} will reflect the currently installed delegate.</li>
 * </ul>
 *
 * @author Principal Software Engineer
 * @since 1.0.0
 * @see CacheManager
 * @see AtomicReference
 */
@Slf4j
public class DelegatingCacheManager implements CacheManager {

    /**
     * Holds the current {@link CacheManager} delegate.
     * <p>
     * Using {@link AtomicReference} ensures atomic, lock-free swaps and proper publication
     * of the new reference to all threads.
     */
    private final AtomicReference<CacheManager> delegateRef;

    /**
     * Create a new delegating cache manager with the given initial delegate.
     *
     * @param initialDelegate the initial {@link CacheManager} delegate (must not be {@code null})
     * @throws IllegalArgumentException if {@code initialDelegate} is {@code null}
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
     * Atomically switch the current delegate to {@code newManager}.
     * <p>
     * <strong>Semantics:</strong> the swap is immediate and visible to subsequent calls. Calls that already
     * fetched the old reference proceed on it; future calls use the new one. This yields zero-downtime behavior.
     * <p>
     * <strong>Lifecycle:</strong> this method does <em>not</em> close or otherwise stop the previous delegate.
     * If cleanup is required, do it explicitly after the switch using the returned old instance (if tracked externally).
     *
     * @param newManager the new {@link CacheManager} to delegate to (must not be {@code null})
     * @throws IllegalArgumentException if {@code newManager} is {@code null}
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
     * Return the current {@link CacheManager} delegate.
     * <p>
     * Useful for diagnostics or advanced integrations (e.g., exposing native metrics).
     *
     * @return the current delegate (never {@code null})
     */
    public CacheManager getCurrentDelegate() {
        return delegateRef.get();
    }

    /**
     * Return the simple class name of the current delegate.
     * <p>
     * Intended for logging/monitoring where a stable human-readable identifier is helpful.
     *
     * @return simple class name of the current delegate
     */
    public String getCurrentDelegateType() {
        return delegateRef.get().getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates directly to the <em>current</em> {@link CacheManager}.
     * The returned {@link Cache} instance represents the cache with the given name in the
     * context of the current provider. See Spring's {@link CacheManager} contract for details.
     *
     * @implNote This method intentionally performs a single {@code get()} on the {@link AtomicReference}
     *           to capture a consistent snapshot of the delegate for the duration of the call.
     */
    @Override
    public Cache getCache(String name) {
        return delegateRef.get().getCache(name);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates directly to the <em>current</em> {@link CacheManager}.
     * The returned collection reflects the cache names known by the current provider.
     * See the Spring {@link CacheManager} Javadoc for the exact semantics.
     */
    @Override
    public Collection<String> getCacheNames() {
        return delegateRef.get().getCacheNames();
    }

    /**
     * Return a human-readable representation with the current delegate type.
     * <p>
     * Useful in logs and thread dumps where identifying the active provider is desirable.
     */
    @Override
    public String toString() {
        return String.format("DelegatingCacheManager{delegate=%s}", getCurrentDelegateType());
    }
}
