package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.grpc.ConfigControlServiceGrpc;
import com.vng.zing.zcm.grpc.HeartbeatRequest;
import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import com.vng.zing.zcm.pingconfig.auth.ClientCredentialsTokenService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * gRPC implementation of the ping strategy.
 * <p>
 * This strategy sends heartbeat messages using gRPC calls to the control service.
 * It uses Protocol Buffers for efficient serialization and HTTP/2 for transport.
 * <p>
 * Requires client credentials authentication (Keycloak client credentials flow).
 * The Bearer token is included in gRPC metadata headers.
 */
@Slf4j
public class GrpcPingStrategy implements PingStrategy {
  
  private static final int TIMEOUT_SECONDS = 5;
  private static final int DEFAULT_GRPC_PORT = 9091;
  private static final int SHUTDOWN_TIMEOUT_SECONDS = 1;
  private static final Metadata.Key<String> AUTHORIZATION_KEY = 
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  private final SdkProperties sdkProperties;
  private final ClientCredentialsTokenService tokenService;

  /**
   * Constructor with SdkProperties and token service.
   *
   * @param sdkProperties SDK configuration properties
   * @param tokenService Client credentials token service for authentication
   */
  public GrpcPingStrategy(SdkProperties sdkProperties, ClientCredentialsTokenService tokenService) {
    this.sdkProperties = sdkProperties;
    this.tokenService = tokenService;
  }

  /**
   * Constructor with SdkProperties only (creates token service).
   * 
   * @param sdkProperties SDK configuration properties
   * @throws IllegalStateException if client credentials are required but not configured
   */
  public GrpcPingStrategy(SdkProperties sdkProperties) {
    this.sdkProperties = sdkProperties;
    // Token service will be created lazily if needed
    // For now, we'll validate configuration
    SdkProperties.Ping.ClientCredentials clientCredentials = sdkProperties != null
            && sdkProperties.getPing() != null
            ? sdkProperties.getPing().getClientCredentials()
            : null;
    
    if (clientCredentials == null || clientCredentials.isRequired()) {
      validateClientCredentials(clientCredentials);
      // Note: Token service requires RestClient, which we don't have here
      // This constructor is mainly for backward compatibility
      throw new IllegalStateException(
              "GrpcPingStrategy requires ClientCredentialsTokenService. " +
                      "Use constructor with tokenService parameter.");
    }
    this.tokenService = null;
  }
  
  @Override
  public void sendHeartbeat(String endpoint, HeartbeatPayload payload) throws Exception {
    String[] parts = endpoint.split(":");
    String host = parts[0];
    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_GRPC_PORT;
    
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .build();
    
    try {
      ConfigControlServiceGrpc.ConfigControlServiceBlockingStub stub = 
          ConfigControlServiceGrpc.newBlockingStub(channel);
      
      // Add Bearer token in gRPC metadata if token service is available
      if (tokenService != null) {
        try {
          String accessToken = tokenService.getAccessToken();
          Metadata headers = new Metadata();
          headers.put(AUTHORIZATION_KEY, "Bearer " + accessToken);
          stub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
          log.debug("Including Bearer token in gRPC metadata for heartbeat request");
        } catch (Exception e) {
          log.error("Failed to obtain access token for gRPC heartbeat authentication", e);
          throw new RuntimeException("Failed to authenticate gRPC heartbeat request: " + e.getMessage(), e);
        }
      } else {
        log.warn("No token service configured for gRPC ping - authentication may fail");
      }
      
      HeartbeatRequest request = convertToGrpc(payload);
      stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .recordHeartbeat(request);
      
      log.debug("gRPC ping sent to {}:{}", host, port);
    } catch (StatusRuntimeException e) {
      throw new Exception("gRPC call failed: " + e.getStatus().getDescription(), e);
    } catch (Exception e) {
      throw new Exception("gRPC communication error: " + e.getMessage(), e);
    } finally {
      channel.shutdown();
      try {
        if (!channel.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          channel.shutdownNow();
        }
      } catch (InterruptedException e) {
        channel.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Validates client credentials configuration.
   *
   * @param clientCredentials client credentials configuration
   * @throws IllegalStateException if validation fails
   */
  private void validateClientCredentials(SdkProperties.Ping.ClientCredentials clientCredentials) {
    if (clientCredentials == null) {
      throw new IllegalStateException(
              "Client credentials are required for gRPC ping authentication. " +
                      "Please configure zcm.sdk.ping.client-credentials.client-id and " +
                      "zcm.sdk.ping.client-credentials.client-secret. " +
                      "Obtain credentials from Admin Dashboard after service approval.");
    }

    if (!StringUtils.hasText(clientCredentials.getClientId())) {
      throw new IllegalStateException(
              "Client ID is required for gRPC ping authentication. " +
                      "Set zcm.sdk.ping.client-credentials.client-id or " +
                      "ZCM_SDK_PING_CLIENT_CREDENTIALS_CLIENT_ID environment variable.");
    }

    if (!StringUtils.hasText(clientCredentials.getClientSecret())) {
      throw new IllegalStateException(
              "Client secret is required for gRPC ping authentication. " +
                      "Set zcm.sdk.ping.client-credentials.client-secret or " +
                      "ZCM_SDK_PING_CLIENT_CREDENTIALS_CLIENT_SECRET environment variable.");
    }

    // Validate token endpoint can be constructed
    if (!StringUtils.hasText(clientCredentials.getTokenEndpoint())
            && !StringUtils.hasText(clientCredentials.getKeycloakUrl())) {
      throw new IllegalStateException(
              "Keycloak token endpoint not configured. " +
                      "Either set zcm.sdk.ping.client-credentials.token-endpoint or " +
                      "set zcm.sdk.ping.client-credentials.keycloak-url.");
    }
  }
  
  @Override
  public String getName() {
    return "gRPC";
  }
  
  @Override
  public PingProtocol getProtocol() {
    return PingProtocol.GRPC;
  }
  
  /**
   * Converts HeartbeatPayload to gRPC HeartbeatRequest.
   * 
   * @param payload the heartbeat payload
   * @return gRPC request object
   */
  private HeartbeatRequest convertToGrpc(HeartbeatPayload payload) {
    return HeartbeatRequest.newBuilder()
        .setServiceName(payload.getServiceName())
        .setInstanceId(payload.getInstanceId())
        .setConfigHash(payload.getConfigHash())
        .setHost(payload.getHost())
        .setPort(payload.getPort())
        .setEnvironment(payload.getEnvironment())
        .setVersion(payload.getVersion())
        .putAllMetadata(payload.getMetadata())
        .build();
  }
}
