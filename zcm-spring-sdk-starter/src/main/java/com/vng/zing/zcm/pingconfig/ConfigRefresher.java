package com.vng.zing.zcm.pingconfig;

import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * Deprecated: Spring Cloud Bus refreshes configuration. Keep as optional helper.
 */
@Deprecated
public class ConfigRefresher {

  private final ContextRefresher refresher;
  private final ConfigHashCalculator hash;

  public ConfigRefresher(ContextRefresher refresher, ConfigHashCalculator hash) {
    this.refresher = refresher;
    this.hash = hash;
  }

  /**
   * Trigger refresh; trả về danh sách keys đã thay đổi (nếu framework cung cấp)
   */
  public Set<String> refresh() {
    Set<String> keys = refresher.refresh();
    // keys may be empty if no changes or implementation specifics
    return CollectionUtils.isEmpty(keys) ? Set.of() : keys;
  }

  public String currentHash() {
    return hash.currentHash();
  }
}
