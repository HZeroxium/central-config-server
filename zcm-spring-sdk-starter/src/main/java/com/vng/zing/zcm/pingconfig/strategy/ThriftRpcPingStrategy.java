package com.vng.zing.zcm.pingconfig.strategy;

import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import com.vng.zing.zcm.thrift.ConfigControlService;
import com.vng.zing.zcm.thrift.HeartbeatRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Apache Thrift RPC implementation of the ping strategy.
 * <p>
 * This strategy sends heartbeat messages using Apache Thrift RPC calls to the
 * control service. It uses binary protocol over TCP sockets for efficient
 * communication.
 */
@Slf4j
public class ThriftRpcPingStrategy implements PingStrategy {
  
  private static final int TIMEOUT_MS = 5000;
  private static final int DEFAULT_THRIFT_PORT = 9090;
  
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
