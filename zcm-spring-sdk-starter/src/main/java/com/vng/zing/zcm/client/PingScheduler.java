package com.vng.zing.zcm.client;

import com.vng.zing.zcm.config.SdkProperties;
import org.springframework.scheduling.annotation.Scheduled;

public class PingScheduler {

  private final SdkProperties props;
  private final PingSender sender;

  public PingScheduler(SdkProperties props, PingSender sender) {
    this.props = props;
    this.sender = sender;
  }

  @Scheduled(fixedDelayString = "${zcm.sdk.ping.fixed-delay:30000}")
  public void tick() {
    if (props.getPing().isEnabled())
      sender.send();
  }
}
