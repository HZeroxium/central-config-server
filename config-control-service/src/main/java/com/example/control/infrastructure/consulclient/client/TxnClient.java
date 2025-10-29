package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.WriteOptions;
import com.example.control.infrastructure.consulclient.model.TxnOp;
import com.example.control.infrastructure.consulclient.model.TxnResult;

import java.util.List;

/**
 * Client for Consul transaction operations.
 */
public interface TxnClient {

    /**
     * Execute a transaction with multiple operations.
     *
     * @param operations list of transaction operations
     * @param options    write options
     * @return response with transaction result
     */
    ConsulResponse<TxnResult> execute(List<TxnOp> operations, WriteOptions options);

    /**
     * Execute a transaction with default options.
     *
     * @param operations list of transaction operations
     * @return response with transaction result
     */
    default ConsulResponse<TxnResult> execute(List<TxnOp> operations) {
        return execute(operations, null);
    }
}
