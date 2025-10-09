package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.pingconfig.HeartbeatPayload;

/**
 * Strategy interface for sending heartbeat messages using different protocols.
 * <p>
 * This interface defines the contract that all ping protocol implementations
 * must follow. It enables the SDK to support multiple communication protocols
 * (HTTP REST, Thrift RPC, gRPC) while maintaining a consistent interface.
 */
public interface PingStrategy {
  
  /**
   * Send heartbeat to control service using the specific protocol implementation.
   * 
   * @param endpoint Target endpoint (URL for HTTP, host:port for Thrift/gRPC)
   * @param payload Heartbeat data to send
   * @throws Exception if ping fails (network errors, protocol errors, etc.)
   */
  void sendHeartbeat(String endpoint, HeartbeatPayload payload) throws Exception;
  
  /**
   * Returns the human-readable name of this strategy for logging purposes.
   * 
   * @return strategy name (e.g., "HTTP REST", "Thrift RPC", "gRPC")
   */
  String getName();
  
  /**
   * Returns the protocol identifier for this strategy.
   * 
   * @return the protocol enum value
   */
  PingProtocol getProtocol();
}
