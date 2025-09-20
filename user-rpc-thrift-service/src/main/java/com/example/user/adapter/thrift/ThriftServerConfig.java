package com.example.user.adapter.thrift;

import com.example.user.thrift.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thrift server bootstrap configuration.
 * <p>
 * Exposes a {@link TServer} via a background daemon thread to handle RPC calls using a
 * {@link TThreadPoolServer} and {@link TBinaryProtocol}. The handler delegates to
 * {@link com.example.user.service.port.UserServicePort}.
 * 
 * Enhanced with comprehensive profiling and metrics collection.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("thrift-server")
public class ThriftServerConfig implements ApplicationRunner {

  @Value("${thrift.port:9090}")
  private int thriftPort;

  private final UserServiceHandler userServiceHandler;

  /**
   * Create the Thrift {@link TProcessor} for the user service.
   *
   * @return configured Thrift processor
   */
  @Bean
  public TProcessor userServiceProcessor() {
    log.info("Creating Thrift processor for UserService with profiling enabled");
    TProcessor processor = new UserService.Processor<>(userServiceHandler);
    log.debug("Thrift processor created successfully with metrics integration");
    return processor;
  }

  /**
   * Start the Thrift server asynchronously on the configured port.
   *
   * @param args application arguments
   * @throws Exception if server start fails
   */
  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("Starting Thrift server on port: {} with profiling enabled", thriftPort);
    try {
      TServerTransport serverTransport = new TServerSocket(thriftPort);
      log.debug("Thrift server transport created on port: {}", thriftPort);
      
      TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport)
          .processor(userServiceProcessor())
          .protocolFactory(new TBinaryProtocol.Factory());
      log.debug("Thrift server arguments configured");
      
      TServer server = new TThreadPoolServer(serverArgs);
      log.debug("Thrift server instance created");
      
      Thread serverThread = new Thread(() -> {
        try {
          log.info("Thrift server starting to serve requests on port: {} with profiling", thriftPort);
          server.serve();
        } catch (Exception e) {
          log.error("Thrift server failed to start or serve requests", e);
        }
      }, "thrift-server");
      
      serverThread.setDaemon(true);
      serverThread.start();
      log.info("Thrift server thread started successfully on port: {} with profiling", thriftPort);
    } catch (Exception e) {
      log.error("Failed to start Thrift server on port: {}", thriftPort, e);
      throw e;
    }
  }
}