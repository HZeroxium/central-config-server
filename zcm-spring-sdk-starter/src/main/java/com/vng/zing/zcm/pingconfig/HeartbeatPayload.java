package com.vng.zing.zcm.pingconfig;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * Represents the heartbeat payload sent to the control service.
 * <p>
 * This DTO contains all the information needed for configuration drift
 * detection
 * and service instance tracking. It's used consistently across all ping
 * protocols
 * (HTTP REST, Thrift RPC, gRPC).
 */
@Data
@Builder
public class HeartbeatPayload {

  /** Service name identifier */
  private String serviceName;

  /** Unique instance identifier */
  private String instanceId;

  /** SHA-256 hash of applied configuration */
  private String configHash;

  /** Instance host address */
  private String host;

  /** Instance port number */
  private Integer port;

  /** Deployment environment (e.g., dev, staging, prod) */
  private String environment;

  /** Service version */
  private String version;

  /** Additional instance metadata */
  private Map<String, String> metadata;
}
