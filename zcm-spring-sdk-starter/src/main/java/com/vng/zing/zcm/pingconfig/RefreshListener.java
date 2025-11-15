package com.vng.zing.zcm.pingconfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Deprecated: Prefer Spring Cloud Bus /actuator/busrefresh.
 */
@Deprecated
@Slf4j
@RequiredArgsConstructor
public class RefreshListener {

  private final ConfigRefresher refresher;
  
  @Autowired(required = false)
  private CacheManager cacheManager;

  @KafkaListener(topics = "#{@zcmRefreshTopic}", containerFactory = "kafkaListenerContainerFactory", autoStartup = "#{@zcmRefreshAutoStartup}")
  public void onRefreshMessage(String msg) {
    try {
      log.info("ZCM refresh event received: {}", msg);
      
      // Invalidate config hash cache before refresh to ensure fresh hash calculation
      if (cacheManager != null) {
        var cache = cacheManager.getCache("config-hash-cache");
        if (cache != null) {
          cache.clear();
          log.debug("Invalidated config-hash-cache due to refresh event");
        }
      }
      
      var keys = refresher.refresh();
      log.info("ZCM refresh applied; changedKeys={}, newHash={}", keys, refresher.currentHash());
    } catch (Exception e) {
      log.warn("ZCM refresh failed: {}", e.getMessage());
    }
  }

}
