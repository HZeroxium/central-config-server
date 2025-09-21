package com.example.thriftserver.config;

import com.example.user.thrift.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ThriftServerConfig implements ApplicationRunner {

  private final ThriftProperties thriftProperties;
  private final UserService.Iface handler;

  @Bean
  public TProcessor userServiceProcessor() {
    return new UserService.Processor<>(handler);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    TServerTransport serverTransport = new TServerSocket(thriftProperties.getPort());
    TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport)
        .processor(userServiceProcessor())
        .protocolFactory(new TBinaryProtocol.Factory())
        .minWorkerThreads(thriftProperties.getMinThreads())
        .maxWorkerThreads(thriftProperties.getMaxThreads());

    TServer server = new TThreadPoolServer(serverArgs);
    Thread serverThread = new Thread(server::serve, thriftProperties.getServerName());
    serverThread.setDaemon(true);
    serverThread.start();
    log.info("Thrift server started on port {} with {} threads",
        thriftProperties.getPort(), thriftProperties.getMaxThreads());
  }
}
