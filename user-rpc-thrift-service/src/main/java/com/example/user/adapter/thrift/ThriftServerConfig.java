package com.example.user.adapter.thrift;

import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.UserService;
import lombok.RequiredArgsConstructor;
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

@Configuration
@RequiredArgsConstructor
public class ThriftServerConfig implements ApplicationRunner {

  @Value("${thrift.port:9090}")
  private int thriftPort;

  private final UserServicePort userServicePort;

  @Bean
  public TProcessor userServiceProcessor() {
    return new UserService.Processor<>(new UserServiceHandler(userServicePort));
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    TServerTransport serverTransport = new TServerSocket(thriftPort);
    TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport)
        .processor(userServiceProcessor())
        .protocolFactory(new TBinaryProtocol.Factory());
    TServer server = new TThreadPoolServer(serverArgs);
    Thread serverThread = new Thread(server::serve, "thrift-server");
    serverThread.setDaemon(true);
    serverThread.start();
  }
}
