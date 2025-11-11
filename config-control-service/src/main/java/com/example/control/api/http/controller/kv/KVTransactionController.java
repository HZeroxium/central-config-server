package com.example.control.api.http.controller.kv;

import com.example.control.api.http.dto.kv.KVDtos;
import com.example.control.api.http.mapper.kv.KVApiMapper;
import com.example.control.application.service.KVService;
import com.example.control.domain.model.kv.KVTransactionRequest;
import com.example.control.domain.model.kv.KVTransactionResponse;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.config.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing low-level transaction endpoint for KV operations.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/application-services/{serviceId}/kv")
@Tag(name = "Key-Value Store", description = "Transactional operations for Key-Value store")
public class KVTransactionController {

    private final KVService kvService;
    private final PrefixPolicy prefixPolicy;

    @PostMapping("/txn")
    @Operation(
            summary = "Execute KV transaction",
            description = "Executes multiple KV operations atomically using Consul's transaction endpoint.",
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "executeKVTransaction"
    )
    public ResponseEntity<KVDtos.TransactionResponse> executeTransaction(
            @PathVariable String serviceId,
            @Valid @RequestBody KVDtos.TransactionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);
        KVTransactionRequest domainRequest = KVApiMapper.toTransactionRequest(serviceId, request, prefixPolicy);
        log.info("Executing KV transaction for service {}", serviceId);

        KVTransactionResponse response = kvService.executeTransaction(serviceId, domainRequest, userContext);
        KVDtos.TransactionResponse dto = KVApiMapper.toTransactionResponse(serviceId, response, prefixPolicy);
        return ResponseEntity.ok(dto);
    }
}


