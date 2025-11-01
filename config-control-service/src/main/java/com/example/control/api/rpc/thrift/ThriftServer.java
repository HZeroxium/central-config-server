package com.example.control.api.rpc.thrift;

import com.example.control.thrift.ConfigControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThriftServer {

    private final ThriftHeartbeatHandler handler;

    @Value("${thrift.server.port:9090}")
    private int port;

    private TServer server;

    /**
     * Starts the Thrift server asynchronously.
     * <p>
     * Uses the dedicated rpcExecutor thread pool for server startup operations.
     * This ensures server startup does not block the main application thread.
     * </p>
     */
    @Async("rpcExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        try {
            TServerSocket serverTransport = new TServerSocket(port);
            ConfigControlService.Processor<ThriftHeartbeatHandler> processor = new ConfigControlService.Processor<>(
                    handler);

            TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport)
                    .processor(processor)
                    .minWorkerThreads(5)
                    .maxWorkerThreads(50);

            server = new TThreadPoolServer(args);

            log.info("Starting Thrift server on port {}", port);
            server.serve();

        } catch (Exception e) {
            log.error("Failed to start Thrift server", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null && server.isServing()) {
            log.info("Stopping Thrift server");
            server.stop();
        }
    }

    public boolean isServing() {
        return server != null && server.isServing();
    }
}
