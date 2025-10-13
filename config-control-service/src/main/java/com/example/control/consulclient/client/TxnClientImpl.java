package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.HttpTransport;
import com.example.control.consulclient.core.WriteOptions;
import com.example.control.consulclient.exception.ConsulException;
import com.example.control.consulclient.model.TxnOp;
import com.example.control.consulclient.model.TxnResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        
        try {
            String path = "/v1/txn";
            
            // Build transaction request with correct Consul schema
            ObjectNode txnRequest = objectMapper.createObjectNode();
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
            
            txnRequest.set("Operations", operationsArray);
            
            // Execute the transaction
            ConsulResponse<JsonNode> response = httpTransport.put(path, txnRequest, options, JsonNode.class);
            
            JsonNode responseNode = response.getBody();
            if (responseNode == null) {
                throw new ConsulException("Empty transaction response");
            }
            
            // Parse the response
            TxnResult txnResult = parseTransactionResult(responseNode);
            
            return ConsulResponse.of(txnResult, response.getConsulIndex());
            
        } catch (Exception e) {
            log.error("Failed to execute transaction", e);
            throw new ConsulException("Failed to execute transaction", e);
        }
    }
    
    /**
     * Parse transaction result from Consul response.
     */
    private TxnResult parseTransactionResult(JsonNode responseNode) {
        try {
            // Parse results
            List<TxnResult.TxnOpResult> results = List.of();
            if (responseNode.has("Results") && responseNode.get("Results").isArray()) {
                ArrayNode resultsArray = (ArrayNode) responseNode.get("Results");
                results = objectMapper.convertValue(resultsArray, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TxnResult.TxnOpResult.class));
            }
            
            // Parse errors
            List<TxnResult.TxnError> errors = List.of();
            if (responseNode.has("Errors") && responseNode.get("Errors").isArray()) {
                ArrayNode errorsArray = (ArrayNode) responseNode.get("Errors");
                errors = objectMapper.convertValue(errorsArray,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TxnResult.TxnError.class));
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
