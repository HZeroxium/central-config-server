package com.vng.zing.zcm.pingconfig.strategy;

/**
 * Enumeration of supported ping protocols.
 * <p>
 * Each protocol represents a different communication mechanism for sending
 * heartbeat messages to the control service.
 */
public enum PingProtocol {
  
  /** HTTP REST API protocol */
  HTTP("http"),
  
  /** Apache Thrift RPC protocol */
  THRIFT("thrift"),
  
  /** gRPC protocol */
  GRPC("grpc"),
  
  /** Apache Kafka messaging protocol */
  KAFKA("kafka");
  
  private final String value;
  
  PingProtocol(String value) {
    this.value = value;
  }
  
  /**
   * Returns the string value of this protocol.
   *
   * @return the string representation of the protocol
   */
  public String getValue() {
    return value;
  }
  
  /**
   * Parses a string value into a {@link PingProtocol}.
   * <p>
   * The parsing is case-insensitive and defaults to HTTP if no match is found.
   *
   * @param value the protocol name string
   * @return the corresponding protocol, defaulting to {@link #HTTP}
   */
  public static PingProtocol fromString(String value) {
    if (value == null || value.isEmpty()) {
      return HTTP;
    }
    
    String normalized = value.toLowerCase();
    for (PingProtocol protocol : values()) {
      if (protocol.value.equalsIgnoreCase(normalized)) {
        return protocol;
      }
    }
    
    return HTTP; // default fallback
  }
}
