package com.example.control.infrastructure.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Transaction-aware wrapper for two-level cache that defers L2 writes until
 * transaction commit.
 * <p>
 * When a transaction is active:
 * <ul>
 * <li>L1 writes happen immediately</li>
 * <li>L2 writes are queued and executed AFTER_COMMIT</li>
 * <li>On rollback, queued L2 operations are discarded</li>
 * </ul>
 * <p>
 * This ensures cache consistency with database transactions - cache is only
 * updated
 * when the transaction successfully commits.
 *
 * @since 1.0.0
 */
@Slf4j
public class TransactionAwareTwoLevelCache implements Cache {

  private final Cache delegate;
  private final Cache l2Cache;
  private final String cacheName;

  /**
   * Create a transaction-aware wrapper around a two-level cache.
   *
   * @param delegate  the underlying two-level cache
   * @param l2Cache   the L2 cache (Redis) for deferred writes
   * @param cacheName cache name for logging
   */
  public TransactionAwareTwoLevelCache(Cache delegate, Cache l2Cache, String cacheName) {
    this.delegate = delegate;
    this.l2Cache = l2Cache;
    this.cacheName = cacheName;
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
    return delegate.get(key);
  }

  @Override
  public <T> T get(Object key, Class<T> type) {
    return delegate.get(key, type);
  }

  @Override
  public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
    return delegate.get(key, valueLoader);
  }

  @Override
  public void put(Object key, Object value) {
    // Always write to L1 immediately
    delegate.put(key, value);

    // If L2 exists and transaction is active, defer L2 write
    if (l2Cache != null && TransactionSynchronizationManager.isActualTransactionActive()) {
      deferL2Write(() -> l2Cache.put(key, value));
      log.debug("Deferred L2 write for key: {} in cache: {}", key, cacheName);
    } else if (l2Cache != null) {
      // No transaction - write to L2 immediately
      l2Cache.put(key, value);
    }
  }

  @Override
  public Cache.ValueWrapper putIfAbsent(Object key, Object value) {
    Cache.ValueWrapper result = delegate.putIfAbsent(key, value);

    if (l2Cache != null && TransactionSynchronizationManager.isActualTransactionActive()) {
      deferL2Write(() -> l2Cache.putIfAbsent(key, value));
      log.debug("Deferred L2 putIfAbsent for key: {} in cache: {}", key, cacheName);
    } else if (l2Cache != null) {
      l2Cache.putIfAbsent(key, value);
    }

    return result;
  }

  @Override
  public void evict(Object key) {
    delegate.evict(key);

    if (l2Cache != null && TransactionSynchronizationManager.isActualTransactionActive()) {
      deferL2Evict(() -> l2Cache.evict(key));
      log.debug("Deferred L2 evict for key: {} in cache: {}", key, cacheName);
    } else if (l2Cache != null) {
      l2Cache.evict(key);
    }
  }

  @Override
  public boolean evictIfPresent(Object key) {
    boolean result = delegate.evictIfPresent(key);

    if (l2Cache != null && TransactionSynchronizationManager.isActualTransactionActive()) {
      deferL2Evict(() -> l2Cache.evictIfPresent(key));
      log.debug("Deferred L2 evictIfPresent for key: {} in cache: {}", key, cacheName);
    } else if (l2Cache != null) {
      l2Cache.evictIfPresent(key);
    }

    return result;
  }

  @Override
  public void clear() {
    delegate.clear();

    if (l2Cache != null && TransactionSynchronizationManager.isActualTransactionActive()) {
      deferL2Clear(() -> l2Cache.clear());
      log.debug("Deferred L2 clear for cache: {}", cacheName);
    } else if (l2Cache != null) {
      l2Cache.clear();
    }
  }

  @Override
  public boolean invalidate() {
    boolean result = delegate.invalidate();

    if (l2Cache != null && TransactionSynchronizationManager.isActualTransactionActive()) {
      deferL2Invalidate(() -> l2Cache.invalidate());
      log.debug("Deferred L2 invalidate for cache: {}", cacheName);
    } else if (l2Cache != null) {
      l2Cache.invalidate();
    }

    return result;
  }

  /**
   * Defer L2 write operation until transaction commit.
   */
  private void deferL2Write(Runnable operation) {
    getOrCreateSynchronization().addL2Write(operation);
  }

  /**
   * Defer L2 evict operation until transaction commit.
   */
  private void deferL2Evict(Runnable operation) {
    getOrCreateSynchronization().addL2Evict(operation);
  }

  /**
   * Defer L2 clear operation until transaction commit.
   */
  private void deferL2Clear(Runnable operation) {
    getOrCreateSynchronization().addL2Clear(operation);
  }

  /**
   * Defer L2 invalidate operation until transaction commit.
   */
  private void deferL2Invalidate(Runnable operation) {
    getOrCreateSynchronization().addL2Invalidate(operation);
  }

  /**
   * Get or create transaction synchronization for this cache.
   */
  private CacheTransactionSynchronization getOrCreateSynchronization() {
    String key = "cache-sync-" + cacheName;
    CacheTransactionSynchronization sync = (CacheTransactionSynchronization) TransactionSynchronizationManager
        .getResource(key);

    if (sync == null) {
      sync = new CacheTransactionSynchronization(cacheName);
      TransactionSynchronizationManager.registerSynchronization(sync);
      TransactionSynchronizationManager.bindResource(key, sync);
    }

    return sync;
  }

  /**
   * Transaction synchronization adapter that executes deferred L2 operations on
   * commit.
   */
  private static class CacheTransactionSynchronization implements TransactionSynchronization {

    private final String cacheName;
    private final List<Runnable> l2Writes = new ArrayList<>();
    private final List<Runnable> l2Evicts = new ArrayList<>();
    private final List<Runnable> l2Clears = new ArrayList<>();
    private final List<Runnable> l2Invalidates = new ArrayList<>();

    CacheTransactionSynchronization(String cacheName) {
      this.cacheName = cacheName;
    }

    void addL2Write(Runnable operation) {
      l2Writes.add(operation);
    }

    void addL2Evict(Runnable operation) {
      l2Evicts.add(operation);
    }

    void addL2Clear(Runnable operation) {
      l2Clears.add(operation);
    }

    void addL2Invalidate(Runnable operation) {
      l2Invalidates.add(operation);
    }

    @Override
    public void afterCommit() {
      log.debug("Executing {} deferred L2 operations for cache: {}",
          l2Writes.size() + l2Evicts.size() + l2Clears.size() + l2Invalidates.size(), cacheName);

      // Execute all deferred operations
      l2Writes.forEach(operation -> {
        try {
          operation.run();
        } catch (Exception e) {
          log.error("Failed to execute deferred L2 write for cache: {}", cacheName, e);
        }
      });

      l2Evicts.forEach(operation -> {
        try {
          operation.run();
        } catch (Exception e) {
          log.error("Failed to execute deferred L2 evict for cache: {}", cacheName, e);
        }
      });

      l2Clears.forEach(operation -> {
        try {
          operation.run();
        } catch (Exception e) {
          log.error("Failed to execute deferred L2 clear for cache: {}", cacheName, e);
        }
      });

      l2Invalidates.forEach(operation -> {
        try {
          operation.run();
        } catch (Exception e) {
          log.error("Failed to execute deferred L2 invalidate for cache: {}", cacheName, e);
        }
      });
    }

    @Override
    public void afterCompletion(int status) {
      // Clear all deferred operations after transaction completes (success or
      // rollback)
      l2Writes.clear();
      l2Evicts.clear();
      l2Clears.clear();
      l2Invalidates.clear();
    }
  }
}
