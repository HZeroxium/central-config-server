package com.example.control.domain.model.kv;

import lombok.Builder;

/**
 * Represents a logical transactional operation against the KV store.
 * <p>
 * The abstraction is independent of Consul specifics to keep the application
 * layer portable. Implementations can be translated into Consul transaction
 * verbs (set, delete, check-index, etc.).
 * </p>
 */
public sealed interface KVTransactionOperation
        permits KVTransactionOperation.SetOperation, KVTransactionOperation.DeleteOperation {

    /**
     * @return absolute key path targeted by the operation
     */
    String key();

    /**
     * @return logical type of the target key
     */
    KVType targetType();

    /**
     * Operation that writes/updates the value associated with a key.
     */
    @Builder
    record SetOperation(
            String key,
            byte[] value,
            Long flags,
            Long cas,
            KVType targetType
    ) implements KVTransactionOperation {

        public boolean isCas() {
            return cas != null;
        }

        public long flagsOrDefault() {
            return flags != null ? flags : targetType.getFlagValue();
        }
    }

    /**
     * Operation that removes a key, optionally guarded by CAS.
     */
    @Builder
    record DeleteOperation(
            String key,
            Long cas,
            boolean recurse,
            KVType targetType
    ) implements KVTransactionOperation {

        public boolean isCas() {
            return cas != null;
        }
    }
}


