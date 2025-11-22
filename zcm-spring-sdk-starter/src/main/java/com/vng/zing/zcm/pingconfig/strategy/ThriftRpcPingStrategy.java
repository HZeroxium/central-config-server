package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import com.vng.zing.zcm.thrift.ConfigControlService;
import com.vng.zing.zcm.thrift.HeartbeatRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.springframework.util.StringUtils;

/**
 * Apache Thrift RPC implementation of the ping strategy.
 * <p>
 * This strategy sends heartbeat messages using Apache Thrift RPC calls to the
 * control service. It uses binary protocol over TCP sockets for efficient
 * communication.
 * <p>
 * <strong>Authentication Note:</strong>
 * Thrift TSocket (raw TCP) does not natively support HTTP headers for authentication.
 * Client credentials are validated at startup, but authentication must be handled
 * at the application layer (e.g., via custom Thrift protocol extensions or server-side
 * IP-based authentication). For full client credentials support, consider using
 * HTTP or gRPC protocols instead.
 */
@Slf4j
public class ThriftRpcPingStrategy implements PingStrategy {

  private static final int TIMEOUT_MS = 5000;
  private static final int DEFAULT_THRIFT_PORT = 9090;

  private final SdkProperties sdkProperties;

  /**
   * Constructor with SdkProperties.
   *
   * @param sdkProperties SDK configuration properties
   */
  public ThriftRpcPingStrategy(SdkProperties sdkProperties) {
    this.sdkProperties = sdkProperties;
    validateClientCredentials();
  }

  /**
   * Default constructor (for backward compatibility).
   * <p>
   * Note: Client credentials validation will be skipped, but authentication
   * may fail at runtime if server requires it.
   */
  public ThriftRpcPingStrategy() {
    this.sdkProperties = null;
    log.warn("ThriftRpcPingStrategy created without SdkProperties. " +
            "Client credentials validation skipped. Authentication may fail.");
  }

  /**
   * Validates that client credentials are configured (even though Thrift doesn't use them directly).
   * This ensures consistency with other protocols and provides clear error messages.
   */
  private void validateClientCredentials() {
    if (sdkProperties == null) {
      return; // Skip validation if properties not available
    }

    SdkProperties.Ping.ClientCredentials clientCredentials = sdkProperties.getPing() != null
            ? sdkProperties.getPing().getClientCredentials()
            : null;

    if (clientCredentials == null || clientCredentials.isRequired()) {
      if (clientCredentials == null || !StringUtils.hasText(clientCredentials.getClientId())) {
        log.warn(
                "Client credentials not configured for Thrift ping. " +
                        "Thrift TSocket does not support HTTP headers, so authentication must be " +
                        "handled at the application layer. Consider using HTTP or gRPC protocols " +
                        "for full client credentials support.");
      }
    }
  }

  @Override
  public void sendHeartbeat(String endpoint, HeartbeatPayload payload) throws Exception {
    String[] parts = endpoint.split(":");
    String host = parts[0];
    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_THRIFT_PORT;

    try (TTransport transport = new TSocket(host, port, TIMEOUT_MS)) {
      transport.open();
      TBinaryProtocol protocol = new TBinaryProtocol(transport);
      ConfigControlService.Client client = new ConfigControlService.Client(protocol);

      HeartbeatRequest request = convertToThrift(payload);
      client.recordHeartbeat(request);

      log.debug("Thrift RPC ping sent to {}:{}", host, port);
    } catch (TTransportException e) {
      throw new Exception("Failed to connect to Thrift service at " + host + ":" + port, e);
    } catch (Exception e) {
      throw new Exception("Thrift RPC call failed: " + e.getMessage(), e);
    }
  }

  @Override
  public String getName() {
    return "Thrift RPC";
  }

  @Override
  public PingProtocol getProtocol() {
    return PingProtocol.THRIFT;
  }

  /**
   * Converts HeartbeatPayload to Thrift HeartbeatRequest.
   * 
   * @param payload the heartbeat payload
   * @return Thrift request object
   */
  private HeartbeatRequest convertToThrift(HeartbeatPayload payload) {
    HeartbeatRequest request = new HeartbeatRequest();
    request.setServiceName(payload.getServiceName());
    request.setInstanceId(payload.getInstanceId());
    request.setConfigHash(payload.getConfigHash());
    request.setHost(payload.getHost());
    request.setPort(payload.getPort());
    request.setEnvironment(payload.getEnvironment());
    request.setVersion(payload.getVersion());
    request.setMetadata(payload.getMetadata());
    return request;
  }
}
