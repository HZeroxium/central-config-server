package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.HttpTransport;
import com.example.control.infrastructure.consulclient.core.WriteOptions;
import com.example.control.infrastructure.consulclient.exception.ConsulException;
import com.example.control.infrastructure.consulclient.model.TxnOp;
import com.example.control.infrastructure.consulclient.model.TxnResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of TxnClient using HttpTransport with correct Consul transaction schema.
 */
@Slf4j
@RequiredArgsConstructor
public class TxnClientImpl implements TxnClient {

    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;

    @Override
    public ConsulResponse<TxnResult> execute(List<TxnOp> operations, WriteOptions options) {
        log.debug("Executing transaction with {} operations", operations.size());

        if (operations.isEmpty()) {
            return ConsulResponse.<TxnResult>builder()
                    .body(TxnResult.builder()
                            .results(List.of())
                            .errors(List.of())
                            .build())
                    .build();
        }

        String path = "/v1/txn";

        // Build transaction request as JSON array (Consul API expects array directly)
        ArrayNode operationsArray = objectMapper.createArrayNode();

        for (TxnOp operation : operations) {
            ObjectNode operationWrapper = objectMapper.createObjectNode();

            switch (operation) {
                case TxnOp.KVSet kvSet -> {
                    ObjectNode kvOp = objectMapper.createObjectNode();
                    kvOp.put("Verb", kvSet.verb());
                    kvOp.put("Key", kvSet.key());
                    if (kvSet.value() != null) {
                        kvOp.put("Value", kvSet.value());
                    }
                    if (kvSet.flags() != null) {
                        kvOp.put("Flags", kvSet.flags());
                    }
                    if (kvSet.index() != null) {
                        kvOp.put("Index", kvSet.index());
                    }
                    if (kvSet.session() != null) {
                        kvOp.put("Session", kvSet.session());
                    }
                    operationWrapper.set("KV", kvOp);
                }
                case TxnOp.KVGet kvGet -> {
                    ObjectNode kvOp = objectMapper.createObjectNode();
                    kvOp.put("Verb", kvGet.verb());
                    kvOp.put("Key", kvGet.key());
                    if (kvGet.index() != null) {
                        kvOp.put("Index", kvGet.index());
                    }
                    operationWrapper.set("KV", kvOp);
                }
                case TxnOp.KVDelete kvDelete -> {
                    ObjectNode kvOp = objectMapper.createObjectNode();
                    kvOp.put("Verb", kvDelete.verb());
                    kvOp.put("Key", kvDelete.key());
                    if (kvDelete.index() != null) {
                        kvOp.put("Index", kvDelete.index());
                    }
                    operationWrapper.set("KV", kvOp);
                }
                case TxnOp.KVCheckIndex kvCheckIndex -> {
                    ObjectNode kvOp = objectMapper.createObjectNode();
                    kvOp.put("Verb", kvCheckIndex.verb());
                    kvOp.put("Key", kvCheckIndex.key());
                    kvOp.put("Index", kvCheckIndex.index());
                    operationWrapper.set("KV", kvOp);
                }
                case TxnOp.KVCheckSession kvCheckSession -> {
                    ObjectNode kvOp = objectMapper.createObjectNode();
                    kvOp.put("Verb", kvCheckSession.verb());
                    kvOp.put("Key", kvCheckSession.key());
                    kvOp.put("Session", kvCheckSession.session());
                    operationWrapper.set("KV", kvOp);
                }
                default -> {
                    log.warn("Unknown transaction operation type: {}", operation.getClass().getSimpleName());
                    continue;
                }
            }

            operationsArray.add(operationWrapper);
        }

        try {
            // Execute the transaction - send array directly as request body
            ConsulResponse<JsonNode> response = httpTransport.put(path, operationsArray, options, JsonNode.class);

            JsonNode responseNode = response.getBody();
            if (responseNode == null) {
                throw new ConsulException("Empty transaction response");
            }

            // Parse the response
            TxnResult txnResult = parseTransactionResult(responseNode);

            return ConsulResponse.<TxnResult>builder()
                    .body(txnResult)
                    .consulIndex(response.getConsulIndex())
                    .knownLeader(response.getKnownLeader())
                    .lastContact(response.getLastContact())
                    .headers(response.getHeaders())
                    .build();

        } catch (HttpClientErrorException e) {
            // Handle HTTP 409 (transaction rolled back) - response body contains transaction result with errors
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.debug("Transaction rolled back (409), parsing error response");
                try {
                    String responseBody = e.getResponseBodyAsString();
                    if (responseBody != null && !responseBody.isEmpty()) {
                        JsonNode responseNode = objectMapper.readTree(responseBody);
                        TxnResult txnResult = parseTransactionResult(responseNode);
                        return ConsulResponse.<TxnResult>builder()
                                .body(txnResult)
                                .build();
                    }
                } catch (Exception parseException) {
                    log.error("Failed to parse 409 error response", parseException);
                    throw new ConsulException("Failed to parse transaction rollback response", parseException);
                }
            }
            // Re-throw other HTTP client errors
            log.error("HTTP error executing transaction: {}", e.getStatusCode(), e);
            throw new ConsulException("Failed to execute transaction: " + e.getMessage(), e);
        } catch (ConsulException e) {
            // Re-throw ConsulException as-is
            throw e;
        } catch (Exception e) {
            log.error("Failed to execute transaction", e);
            throw new ConsulException("Failed to execute transaction", e);
        }
    }

    /**
     * Parse transaction result from Consul response.
     * Consul returns nested structure: {"Results": [{"KV": {"Key": "...", "ModifyIndex": ..., ...}}], "Errors": [...]}
     */
    private TxnResult parseTransactionResult(JsonNode responseNode) {
        try {
            // Parse results - Consul returns nested structure with KV/Node/Service/Check keys
            List<TxnResult.TxnOpResult> results = new ArrayList<>();
            if (responseNode.has("Results") && responseNode.get("Results").isArray()) {
                ArrayNode resultsArray = (ArrayNode) responseNode.get("Results");
                for (JsonNode resultEntry : resultsArray) {
                    // Each result entry has structure: {"KV": {...}} or {"Node": {...}} etc.
                    // For KV operations, extract the KV object
                    if (resultEntry.has("KV")) {
                        JsonNode kvResult = resultEntry.get("KV");
                        TxnResult.TxnOpResult.TxnOpResultBuilder builder = TxnResult.TxnOpResult.builder();

                        // Extract fields from KV result
                        if (kvResult.has("Key")) {
                            builder.key(kvResult.get("Key").asText());
                        }
                        if (kvResult.has("Value") && !kvResult.get("Value").isNull()) {
                            builder.value(kvResult.get("Value").asText());
                        }
                        if (kvResult.has("Flags")) {
                            builder.flags(kvResult.get("Flags").asLong());
                        }
                        // Map ModifyIndex to index field (used by KVTransactionService)
                        if (kvResult.has("ModifyIndex")) {
                            builder.index(kvResult.get("ModifyIndex").asLong());
                        } else if (kvResult.has("CreateIndex")) {
                            // Fallback to CreateIndex if ModifyIndex is not present
                            builder.index(kvResult.get("CreateIndex").asLong());
                        }
                        if (kvResult.has("Session")) {
                            builder.session(kvResult.get("Session").asText());
                        }
                        // Verb is not returned in response, but we can infer it or leave null
                        // Since KVTransactionService only uses index, this is acceptable

                        results.add(builder.build());
                    }
                    // Note: Delete operations don't return results, so they won't appear in the results array
                    // This is handled correctly by KVTransactionService which checks results size
                }
            }

            // Parse errors - these are in flat structure
            List<TxnResult.TxnError> errors = new ArrayList<>();
            if (responseNode.has("Errors") && responseNode.get("Errors").isArray()) {
                ArrayNode errorsArray = (ArrayNode) responseNode.get("Errors");
                for (JsonNode errorEntry : errorsArray) {
                    int opIndex = errorEntry.has("OpIndex") ? errorEntry.get("OpIndex").asInt() : -1;
                    String what = errorEntry.has("What") ? errorEntry.get("What").asText() : "Unknown error";
                    errors.add(TxnResult.TxnError.builder()
                            .opIndex(opIndex)
                            .what(what)
                            .build());
                }
            }

            return TxnResult.builder()
                    .results(results)
                    .errors(errors)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse transaction result", e);
            throw new ConsulException("Failed to parse transaction result", e);
        }
    }
}
