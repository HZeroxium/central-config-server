package com.vng.zing.zcm.pingconfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Deprecated: Prefer Spring Cloud Bus /actuator/busrefresh.
 */
@Deprecated
@Slf4j
@RequiredArgsConstructor
public class RefreshListener {

  private final ConfigRefresher refresher;

  @KafkaListener(topics = "#{@zcmRefreshTopic}", containerFactory = "kafkaListenerContainerFactory", autoStartup = "#{@zcmRefreshAutoStartup}")
  public void onRefreshMessage(String msg) {
    try {
      log.info("ZCM refresh event received: {}", msg);
      var keys = refresher.refresh();
      log.info("ZCM refresh applied; changedKeys={}, newHash={}", keys, refresher.currentHash());
    } catch (Exception e) {
      log.warn("ZCM refresh failed: {}", e.getMessage());
    }
  }

}
