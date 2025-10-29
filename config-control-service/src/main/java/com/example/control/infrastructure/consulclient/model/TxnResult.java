package com.example.control.infrastructure.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

/**
 * Result of a Consul transaction.
 */
@Builder
public record TxnResult(
        @JsonProperty("Results")
        List<TxnOpResult> results,

        @JsonProperty("Errors")
        List<TxnError> errors
) {

    /**
     * Check if the transaction was successful.
     *
     * @return true if no errors occurred
     */
    public boolean isSuccessful() {
        return errors == null || errors.isEmpty();
    }

    /**
     * Individual transaction operation result.
     */
    @Builder
    public record TxnOpResult(
            @JsonProperty("Verb")
            String verb,

            @JsonProperty("Key")
            String key,

            @JsonProperty("Value")
            String value, // Base64 encoded

            @JsonProperty("Flags")
            Long flags,

            @JsonProperty("Index")
            Long index,

            @JsonProperty("Session")
            String session
    ) {

        /**
         * Get the decoded value as bytes.
         *
         * @return decoded value as byte array
         */
        public byte[] getValueBytes() {
            if (value == null || value.isEmpty()) {
                return new byte[0];
            }
            return java.util.Base64.getDecoder().decode(value);
        }
    }

    /**
     * Transaction error.
     */
    @Builder
    public record TxnError(
            @JsonProperty("OpIndex")
            int opIndex,

            @JsonProperty("What")
            String what
    ) {
    }
}
