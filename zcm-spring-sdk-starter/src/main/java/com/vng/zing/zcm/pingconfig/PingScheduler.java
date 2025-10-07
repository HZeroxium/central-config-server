package com.vng.zing.zcm.pingconfig;

import com.vng.zing.zcm.config.SdkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class PingScheduler {

  private final SdkProperties props;
  private final PingSender sender;

  public PingScheduler(SdkProperties props, PingSender sender) {
    this.props = props;
    this.sender = sender;
  }

  @Scheduled(fixedDelayString = "${zcm.sdk.ping.fixed-delay:30000}")
  public void tick() {
    log.debug("ZCM ping scheduler tick - enabled: {}", props.getPing().isEnabled());
    if (props.getPing().isEnabled()) {
      log.info("ZCM ping sending heartbeat to control service");
      sender.send();
    }
  }
}
