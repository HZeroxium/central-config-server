package com.example.control.domain.model.kv;

import java.util.List;

/**
 * Domain request for executing one or more KV operations atomically.
 */
public record KVTransactionRequest(
        String serviceId,
        List<KVTransactionOperation> operations
) {
    public KVTransactionRequest {
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException("operations must not be empty");
        }
    }
}


