package com.example.control.infrastructure.consulclient.core;

/**
 * Consul consistency modes for read operations.
 */
public enum Consistency {
    /**
     * Default consistency - allows stale reads for performance.
     */
    DEFAULT,

    /**
     * Strong consistency - ensures reads are from the leader.
     */
    CONSISTENT,

    /**
     * Stale consistency - allows reads from any server, even if stale.
     */
    STALE
}
