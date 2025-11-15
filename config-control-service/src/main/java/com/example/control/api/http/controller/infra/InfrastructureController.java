package com.example.control.api.http.controller.infra;

import com.example.control.api.http.dto.infra.KafkaConfigDto;
import com.example.control.infrastructure.config.messaging.HeartbeatProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for exposing infrastructure configuration to SDK clients.
 * <p>
 * Provides endpoints for retrieving infrastructure component configuration
 * that SDK clients need to connect (e.g., Kafka bootstrap servers, topic names).
 */
@Slf4j
@RestController
@RequestMapping("/api/infrastructure")
@RequiredArgsConstructor
@Tag(name = "Infrastructure", description = "Infrastructure configuration API for SDK clients")
public class InfrastructureController {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    private final HeartbeatProperties heartbeatProperties;

    /**
     * Exposes Kafka configuration for SDK clients to send heartbeat messages.
     * <p>
     * This endpoint allows SDK clients to dynamically discover Kafka bootstrap
     * servers and topic name without hardcoding configuration. The client can
     * cache this configuration and refresh periodically.
     *
     * @return Kafka configuration with bootstrap servers and topic name
     */
    @GetMapping("/kafka-config")
    @Operation(
            summary = "Get Kafka configuration for heartbeat ping",
            description = """
                    Retrieves Kafka configuration (bootstrap servers and topic name)
                    that SDK clients need to send heartbeat messages via Kafka.
                    
                    **Public Endpoint:** No authentication required for SDK integration
                    **Caching:** Clients should cache this configuration and refresh periodically
                    **Fallback:** Clients should have fallback configuration via properties/env vars
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "getKafkaConfig")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kafka configuration retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KafkaConfigDto.KafkaConfigResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<KafkaConfigDto.KafkaConfigResponse> getKafkaConfig() {
        log.debug("Retrieving Kafka configuration for SDK clients");

        KafkaConfigDto.KafkaConfigResponse response = KafkaConfigDto.KafkaConfigResponse.builder()
                .bootstrapServers(kafkaBootstrapServers)
                .topic(heartbeatProperties.getKafka().getTopic())
                .build();

        log.debug("Returning Kafka config: bootstrapServers={}, topic={}",
                response.getBootstrapServers(), response.getTopic());

        return ResponseEntity.ok(response);
    }
}

