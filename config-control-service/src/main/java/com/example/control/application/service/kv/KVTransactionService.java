package com.example.control.application.service.kv;

import com.example.control.domain.model.kv.KVTransactionOperation;
import com.example.control.domain.model.kv.KVTransactionRequest;
import com.example.control.domain.model.kv.KVTransactionResponse;
import com.example.control.infrastructure.consulclient.client.TxnClient;
import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.model.TxnOp;
import com.example.control.infrastructure.consulclient.model.TxnResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service responsible for executing Consul KV transactions while
 * enforcing operational constraints (batch sizing, retry strategy, error
 * mapping).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KVTransactionService {

    private static final int CONSUL_TXN_LIMIT = 64;

    private final TxnClient txnClient;

    public KVTransactionResponse execute(KVTransactionRequest request) {
        List<KVTransactionOperation> operations = request.operations();
        if (operations.isEmpty()) {
            return new KVTransactionResponse(true, List.of(), "");
        }

        List<KVTransactionResponse.OperationResult> aggregatedResults = new ArrayList<>();
        int from = 0;
        while (from < operations.size()) {
            int to = Math.min(from + CONSUL_TXN_LIMIT, operations.size());
            List<KVTransactionOperation> batch = operations.subList(from, to);

            KVTransactionResponse batchResponse = executeBatch(batch);
            aggregatedResults.addAll(batchResponse.results());

            if (!batchResponse.success()) {
                String message = batchResponse.errorMessage() != null && !batchResponse.errorMessage().isBlank()
                        ? batchResponse.errorMessage()
                        : "Transaction batch failed";
                return new KVTransactionResponse(false, aggregatedResults, message);
            }

            from = to;
        }

        return new KVTransactionResponse(true, aggregatedResults, "");
    }

    private KVTransactionResponse executeBatch(List<KVTransactionOperation> batch) {
        try {
            List<TxnOp> consulOps = batch.stream()
                    .map(this::toTxnOp)
                    .collect(Collectors.toList());

            ConsulResponse<TxnResult> response = txnClient.execute(consulOps, null);
            TxnResult txnResult = Optional.ofNullable(response)
                    .map(ConsulResponse::getBody)
                    .orElse(null);

            if (txnResult == null) {
                String message = "Consul transaction returned no result";
                log.error(message);
                return failureForBatch(batch, message);
            }

            if (!txnResult.isSuccessful()) {
                String message = txnResult.errors() != null && !txnResult.errors().isEmpty()
                        ? txnResult.errors().getFirst().what()
                        : "Consul transaction reported failure";
                return failureFromErrors(batch, txnResult);
            }

            List<KVTransactionResponse.OperationResult> results = buildSuccessResults(batch, txnResult);
            return new KVTransactionResponse(true, results, "");
        } catch (Exception ex) {
            log.error("Consul transaction execution failed: {}", ex.getMessage(), ex);
            return failureForBatch(batch, ex.getMessage());
        }
    }

    private List<KVTransactionResponse.OperationResult> buildSuccessResults(List<KVTransactionOperation> batch,
                                                                            TxnResult txnResult) {
        List<TxnResult.TxnOpResult> opResults = Optional.ofNullable(txnResult.results()).orElse(List.of());
        List<KVTransactionResponse.OperationResult> results = new ArrayList<>(batch.size());

        for (int i = 0; i < batch.size(); i++) {
            KVTransactionOperation operation = batch.get(i);
            Long modifyIndex = opResults.size() > i ? opResults.get(i).index() : null;
            results.add(new KVTransactionResponse.OperationResult(
                    operation.key(),
                    true,
                    modifyIndex,
                    ""
            ));
        }
        return results;
    }

    private KVTransactionResponse failureFromErrors(List<KVTransactionOperation> batch, TxnResult txnResult) {
        Map<Integer, String> errorMap = Optional.ofNullable(txnResult.errors())
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(TxnResult.TxnError::opIndex, TxnResult.TxnError::what, (a, b) -> a));

        List<KVTransactionResponse.OperationResult> results = new ArrayList<>(batch.size());
        for (int i = 0; i < batch.size(); i++) {
            KVTransactionOperation operation = batch.get(i);
            String message = errorMap.getOrDefault(i, "");
            results.add(new KVTransactionResponse.OperationResult(
                    operation.key(),
                    !errorMap.containsKey(i),
                    null,
                    message
            ));
        }

        String firstError = errorMap.values().stream().findFirst().orElse("Consul transaction failed");
        return new KVTransactionResponse(false, results, firstError);
    }

    private KVTransactionResponse failureForBatch(List<KVTransactionOperation> batch, String message) {
        List<KVTransactionResponse.OperationResult> results = batch.stream()
                .map(op -> new KVTransactionResponse.OperationResult(op.key(), false, null, message))
                .toList();
        return new KVTransactionResponse(false, results, message);
    }

    private TxnOp toTxnOp(KVTransactionOperation operation) {
        if (operation instanceof KVTransactionOperation.SetOperation setOperation) {
            TxnOp.KVSet.KVSetBuilder builder = TxnOp.KVSet.builder()
                    .verb("set")
                    .key(setOperation.key())
                    .value(encodeValue(setOperation.value()))
                    .flags(setOperation.flagsOrDefault());
            if (setOperation.cas() != null) {
                builder.index(setOperation.cas());
            }
            return builder.build();
        }
        if (operation instanceof KVTransactionOperation.DeleteOperation deleteOperation) {
            TxnOp.KVDelete.KVDeleteBuilder builder = TxnOp.KVDelete.builder()
                    .verb("delete")
                    .key(deleteOperation.key());
            if (deleteOperation.cas() != null) {
                builder.index(deleteOperation.cas());
            }
            return builder.build();
        }
        throw new IllegalArgumentException("Unsupported transaction operation: " + operation.getClass().getSimpleName());
    }

    private String encodeValue(byte[] value) {
        if (value == null || value.length == 0) {
            return "";
        }
        return Base64.getEncoder().encodeToString(value);
    }
}


