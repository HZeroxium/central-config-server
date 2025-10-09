namespace java com.vng.zing.zcm.thrift

/**
 * Thrift IDL definition for ZCM SDK heartbeat communication.
 * 
 * This file defines the data structures and service interface used for
 * sending heartbeat messages via Apache Thrift RPC protocol.
 */

struct HeartbeatRequest {
  1: required string serviceName,    // Service name identifier
  2: required string instanceId,     // Unique instance identifier
  3: optional string configHash,     // SHA-256 hash of applied configuration
  4: optional string host,           // Instance host address
  5: optional i32 port,              // Instance port number
  6: optional string environment,    // Deployment environment (dev, staging, prod)
  7: optional string version,        // Service version
  8: optional map<string, string> metadata  // Additional instance metadata
}

struct HeartbeatResponse {
  1: required bool success,          // Whether the heartbeat was processed successfully
  2: optional string message,        // Response message from control service
  3: optional i64 timestamp         // Server timestamp when heartbeat was received
}

/**
 * Service interface for heartbeat communication.
 * 
 * This service is implemented by the config-control-service to receive
 * heartbeat messages from SDK-enabled services.
 */
service ConfigControlService {
  /**
   * Record a heartbeat from a service instance.
   * 
   * @param request The heartbeat request containing service metadata
   * @return Response indicating success/failure and optional message
   */
  HeartbeatResponse recordHeartbeat(1: HeartbeatRequest request)
}
