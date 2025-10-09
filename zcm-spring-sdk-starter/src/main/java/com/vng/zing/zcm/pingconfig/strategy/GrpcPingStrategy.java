package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.grpc.ConfigControlServiceGrpc;
import com.vng.zing.zcm.grpc.HeartbeatRequest;
import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

/**
 * gRPC implementation of the ping strategy.
 * <p>
 * This strategy sends heartbeat messages using gRPC calls to the control service.
 * It uses Protocol Buffers for efficient serialization and HTTP/2 for transport.
 */
@Slf4j
public class GrpcPingStrategy implements PingStrategy {
  
  private static final int TIMEOUT_SECONDS = 5;
  private static final int DEFAULT_GRPC_PORT = 9091;
  private static final int SHUTDOWN_TIMEOUT_SECONDS = 1;
  
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
