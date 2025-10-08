package com.vng.zing.zcm.pingconfig;

import com.vng.zing.zcm.config.SdkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * A scheduled task that periodically triggers {@link PingSender#send()}.
 * <p>
 * This ensures the service sends heartbeats to the control plane at a fixed interval,
 * configurable via {@code zcm.sdk.ping.fixed-delay} (default: 30 seconds).
 * <p>
 * The scheduler is resilient — it checks if pinging is enabled before sending.
 */
@Slf4j
public class PingScheduler {

  private final SdkProperties props;
  private final PingSender sender;

  /**
   * Constructs a {@code PingScheduler} with dependencies injected.
   *
   * @param props  SDK configuration properties
   * @param sender the {@link PingSender} responsible for transmitting heartbeats
   */
  public PingScheduler(SdkProperties props, PingSender sender) {
    this.props = props;
    this.sender = sender;
  }

  /**
   * Periodic scheduler entrypoint executed according to the {@code fixedDelayString} property.
   * <p>
   * Logs scheduler state and delegates actual sending to {@link PingSender}.
   * <p>
   * Example:
   * <pre>
   * 2025-10-08 10:30:00 [INFO] ZCM ping sending heartbeat to control service
   * </pre>
   */
  @Scheduled(fixedDelayString = "${zcm.sdk.ping.fixed-delay:30000}")
  public void tick() {
    log.debug("ZCM ping scheduler tick - enabled: {}", props.getPing().isEnabled());
    if (props.getPing().isEnabled()) {
      log.info("ZCM ping sending heartbeat to control service");
      sender.send();
    }
  }
}
