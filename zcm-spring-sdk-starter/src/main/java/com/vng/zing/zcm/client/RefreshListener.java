package com.vng.zing.zcm.client;

import com.vng.zing.zcm.config.SdkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
@RequiredArgsConstructor
public class RefreshListener {

  private final SdkProperties props;
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

  // Bean property resolvers for SpEL - moved to SdkAutoConfiguration
}
