package com.example.control.domain.model.kv;

import java.util.List;
import java.util.Optional;

/**
 * Domain response representing the outcome of a KV transaction.
 */
public record KVTransactionResponse(
        boolean success,
        List<OperationResult> results,
        String errorMessage
) {

    public KVTransactionResponse {
        results = results == null ? List.of() : List.copyOf(results);
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public String errorMessage() {
        return errorMessage;
    }

    /**
     * Result of a single operation within the transaction.
     *
     * @param key         absolute key that was targeted
     * @param success     flag indicating operation success
     * @param modifyIndex Consul modify index if available
     * @param message     optional message describing failure details
     */
    public record OperationResult(
            String key,
            boolean success,
            Long modifyIndex,
            String message
    ) {
        public OperationResult {
            message = message == null ? "" : message;
        }

        public String message() {
            return message;
        }

        public Optional<String> messageOptional() {
            return message.isBlank() ? Optional.empty() : Optional.of(message);
        }
    }
}


