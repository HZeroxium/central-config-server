package com.example.control.api.http.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.control.infrastructure.config.security.SecurityProperties;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication configuration API")
public class AuthConfigController {
    
    private final SecurityProperties securityProperties;
    
    /**
     * Public endpoint to discover Keycloak token endpoint.
     * No authentication required - used by SDK for initial token discovery.
     */
    @GetMapping("/token-endpoint")
    @Operation(summary = "Get Keycloak token endpoint", 
               description = "Public endpoint to discover Keycloak token endpoint for M2M authentication")
    public ResponseEntity<Map<String, String>> getTokenEndpoint() {
        // Extract base URL from issuer URI
        // issuer-uri format: http://keycloak:8080/realms/config-control
        String baseUrl = securityProperties.getJwt().getIssuerUri().replace("/realms/config-control", "");
        String tokenEndpoint = baseUrl + "/realms/config-control/protocol/openid-connect/token";
        
        return ResponseEntity.ok(Map.of("tokenEndpoint", tokenEndpoint));
    }
}
