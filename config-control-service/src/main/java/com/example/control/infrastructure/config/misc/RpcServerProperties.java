package com.example.control.infrastructure.config.misc;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for RPC servers (Thrift and gRPC).
 * <p>
 * Maps properties from {@code rpc.server.*} in application.yml.
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "rpc.server")
public class RpcServerProperties {

    /**
     * Thrift server port.
     */
    @Positive
    private int thriftPort = 9090;

    /**
     * gRPC server port.
     */
    @Positive
    private int grpcPort = 9091;
}

