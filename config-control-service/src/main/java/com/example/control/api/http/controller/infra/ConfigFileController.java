package com.example.control.api.http.controller.infra;

import com.example.control.api.http.dto.infra.ConfigFileDtos;
import com.example.control.application.service.ApplicationServiceService;
import com.example.control.domain.model.ApplicationService;
import com.example.control.infrastructure.configfile.ConfigFileGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for config file generation operations.
 * <p>
 * Provides endpoints to generate Spring Cloud Config Server YAML files
 * for ApplicationServices.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/config-files")
@RequiredArgsConstructor
@Tag(name = "Config Files", description = "Config file generation for Spring Cloud Config Server")
public class ConfigFileController {

    private final ConfigFileGeneratorService generatorService;
    private final ApplicationServiceService applicationServiceService;

    /**
     * Generate config files for all services in the database.
     */
    @PostMapping("/generate")
    @Operation(
            summary = "Generate config files for all services",
            description = "Generates Spring Cloud Config Server YAML files for all ApplicationServices in the database"
    )
    @ApiResponse(responseCode = "200", description = "Config files generated successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<ConfigFileDtos.GenerateAllResponse> generateAll() {
        log.info("Generating config files for all services");

        List<ApplicationService> serviceList = applicationServiceService.findAll();

        ConfigFileGeneratorService.AggregateGenerationResult result =
                generatorService.generateForServices(serviceList);

        ConfigFileDtos.GenerateAllResponse response = ConfigFileDtos.GenerateAllResponse.builder()
                .totalServices(result.totalServices)
                .successCount(result.successCount)
                .failureCount(result.failureCount)
                .totalFilesGenerated(result.totalFilesGenerated)
                .results(result.results.stream()
                        .map(r -> ConfigFileDtos.GenerationResult.builder()
                                .serviceId(r.serviceId)
                                .filesGenerated(r.filesGenerated)
                                .filesSkipped(r.filesSkipped)
                                .error(r.error)
                                .build())
                        .toList())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Generate config files for a specific service.
     */
    @PostMapping("/generate/{serviceId}")
    @Operation(
            summary = "Generate config files for a specific service",
            description = "Generates Spring Cloud Config Server YAML files for the specified ApplicationService"
    )
    @ApiResponse(responseCode = "200", description = "Config files generated successfully")
    @ApiResponse(responseCode = "404", description = "Service not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'TEAM_LEAD')")
    public ResponseEntity<ConfigFileDtos.GenerateResponse> generateForService(
            @Parameter(description = "Service identifier", required = true)
            @PathVariable String serviceId
    ) {
        log.info("Generating config files for service: {}", serviceId);

        ApplicationService service = applicationServiceService.findById(
                com.example.control.domain.valueobject.id.ApplicationServiceId.of(serviceId)
        ).orElseThrow(() -> new RuntimeException("Service not found: " + serviceId));

        ConfigFileGeneratorService.GenerationResult result =
                generatorService.generateForService(service);

        ConfigFileDtos.GenerateResponse response = ConfigFileDtos.GenerateResponse.builder()
                .serviceId(result.serviceId)
                .filesGenerated(result.filesGenerated)
                .filesSkipped(result.filesSkipped)
                .error(result.error)
                .build();

        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Generate config files for HeartbeatLoadTester pattern services.
     */
    @PostMapping("/generate/load-test")
    @Operation(
            summary = "Generate config files for load test services",
            description = "Generates config files for services matching HeartbeatLoadTester pattern (payment-service-1, order-service-2, etc.)"
    )
    @ApiResponse(responseCode = "200", description = "Config files generated successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<ConfigFileDtos.GenerateLoadTestResponse> generateForLoadTest(
            @Valid @RequestBody ConfigFileDtos.GenerateLoadTestRequest request
    ) {
        log.info("Generating config files for load test: {} services, {} instances per service",
                request.getNumServices(), request.getInstancesPerService());

        ConfigFileGeneratorService.AggregateGenerationResult result =
                generatorService.generateForLoadTest(request.getNumServices(), request.getInstancesPerService());

        ConfigFileDtos.GenerateLoadTestResponse response = ConfigFileDtos.GenerateLoadTestResponse.builder()
                .totalServices(result.totalServices)
                .successCount(result.successCount)
                .failureCount(result.failureCount)
                .totalFilesGenerated(result.totalFilesGenerated)
                .build();

        return ResponseEntity.ok(response);
    }
}

