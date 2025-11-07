package com.example.control.benchmark.kv;

import com.example.control.application.service.KVService;
import com.example.control.benchmark.kv.BenchmarkConfig.Operation;
import com.example.control.domain.model.KVEntry;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates benchmark execution for KV operations.
 * <p>
 * Supports benchmarking at both KVService (with permissions) and KVStorePort (direct adapter) layers.
 * Executes operations in parallel, collects statistics, and handles cleanup.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class KVBenchmarkRunner {

    private final BenchmarkConfig config;
    private final HierarchicalDatasetGenerator datasetGenerator;
    private final PrefixPolicy prefixPolicy;

    /**
     * Run benchmark at KVService layer (with permissions).
     *
     * @param kvService KV service
     * @param userContext user context for permissions
     * @return list of operation summaries
     */
    public List<BenchmarkStatistics.Summary> runServiceBenchmark(KVService kvService, UserContext userContext) {
        log.info("Running benchmark at KVService layer");
        HierarchicalDatasetGenerator.Dataset dataset = datasetGenerator.generate();
        log.info("Generated dataset: {} keys under {} prefixes", dataset.keys().size(), dataset.prefixes().size());

        // Pre-populate data
        prePopulateData(kvService, userContext, dataset);

        // Warmup
        warmup(kvService, userContext, dataset);

        // Run benchmarks
        List<BenchmarkStatistics.Summary> summaries = new ArrayList<>();
        for (Operation op : config.operations()) {
            BenchmarkStatistics stats = new BenchmarkStatistics(op.name().toLowerCase());
            long startTime = System.nanoTime();
            runOperation(op, kvService, userContext, dataset, stats);
            long endTime = System.nanoTime();
            double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
            summaries.add(stats.summarize());
            log.info("Completed {}: {} ops in {:.2f}s", op, config.runs(), totalTimeSeconds);
        }

        return summaries;
    }

    /**
     * Run benchmark at KVStorePort layer (direct adapter).
     *
     * @param kvStorePort KV store port
     * @return list of operation summaries
     */
    public List<BenchmarkStatistics.Summary> runPortBenchmark(KVStorePort kvStorePort) {
        log.info("Running benchmark at KVStorePort layer");
        HierarchicalDatasetGenerator.Dataset dataset = datasetGenerator.generate();
        log.info("Generated dataset: {} keys under {} prefixes", dataset.keys().size(), dataset.prefixes().size());

        // Pre-populate data
        prePopulateDataPort(kvStorePort, dataset);

        // Warmup
        warmupPort(kvStorePort, dataset);

        // Run benchmarks
        List<BenchmarkStatistics.Summary> summaries = new ArrayList<>();
        for (Operation op : config.operations()) {
            BenchmarkStatistics stats = new BenchmarkStatistics(op.name().toLowerCase());
            long startTime = System.nanoTime();
            runOperationPort(op, kvStorePort, dataset, stats);
            long endTime = System.nanoTime();
            double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
            summaries.add(stats.summarize());
            log.info("Completed {}: {} ops in {:.2f}s", op, config.runs(), totalTimeSeconds);
        }

        return summaries;
    }

    /**
     * Pre-populate data for benchmark.
     */
    private void prePopulateData(KVService kvService, UserContext userContext, HierarchicalDatasetGenerator.Dataset dataset) {
        log.info("Pre-populating {} keys...", dataset.keys().size());
        int count = 0;
        for (String relativePath : dataset.keys()) {
            try {
                byte[] value = datasetGenerator.generateValue();
                KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder().build();
                kvService.put(config.serviceId(), relativePath, value, writeOptions, userContext);
                count++;
                if (count % 100 == 0) {
                    log.debug("Pre-populated {} keys...", count);
                }
            } catch (Exception e) {
                log.warn("Failed to pre-populate key {}: {}", relativePath, e.getMessage());
            }
        }
        log.info("Pre-populated {} keys", count);
    }

    /**
     * Pre-populate data for port benchmark.
     */
    private void prePopulateDataPort(KVStorePort kvStorePort, HierarchicalDatasetGenerator.Dataset dataset) {
        log.info("Pre-populating {} keys...", dataset.keys().size());
        int count = 0;
        for (String relativePath : dataset.keys()) {
            try {
                byte[] value = datasetGenerator.generateValue();
                String absoluteKey = prefixPolicy.buildAbsoluteKey(config.serviceId(), relativePath);
                KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder().build();
                kvStorePort.put(absoluteKey, value, writeOptions);
                count++;
                if (count % 100 == 0) {
                    log.debug("Pre-populated {} keys...", count);
                }
            } catch (Exception e) {
                log.warn("Failed to pre-populate key {}: {}", relativePath, e.getMessage());
            }
        }
        log.info("Pre-populated {} keys", count);
    }

    /**
     * Warmup phase.
     */
    private void warmup(KVService kvService, UserContext userContext, HierarchicalDatasetGenerator.Dataset dataset) {
        log.info("Warming up with {} operations...", config.warmup());
        for (int i = 0; i < config.warmup(); i++) {
            String relativePath = dataset.keys().get(ThreadLocalRandom.current().nextInt(dataset.keys().size()));
            try {
                // GET
                KVStorePort.KVReadOptions readOptions = KVStorePort.KVReadOptions.builder()
                        .raw(true)
                        .build();
                kvService.get(config.serviceId(), relativePath, readOptions, userContext);
            } catch (Exception e) {
                // Ignore warmup errors
            }
        }
        log.info("Warmup completed");
    }

    /**
     * Warmup phase for port.
     */
    private void warmupPort(KVStorePort kvStorePort, HierarchicalDatasetGenerator.Dataset dataset) {
        log.info("Warming up with {} operations...", config.warmup());
        for (int i = 0; i < config.warmup(); i++) {
            String relativePath = dataset.keys().get(ThreadLocalRandom.current().nextInt(dataset.keys().size()));
            try {
                String absoluteKey = prefixPolicy.buildAbsoluteKey(config.serviceId(), relativePath);
                KVStorePort.KVReadOptions readOptions = KVStorePort.KVReadOptions.builder()
                        .raw(true)
                        .build();
                kvStorePort.get(absoluteKey, readOptions);
            } catch (Exception e) {
                // Ignore warmup errors
            }
        }
        log.info("Warmup completed");
    }

    /**
     * Run operation at service layer.
     */
    private void runOperation(Operation op, KVService kvService, UserContext userContext,
                              HierarchicalDatasetGenerator.Dataset dataset, BenchmarkStatistics stats) {
        ExecutorService executor = Executors.newFixedThreadPool(config.threads());
        CountDownLatch latch = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < config.runs(); i++) {
            Thread thread = new Thread(() -> {
                try {
                    latch.await();
                    executeOperation(op, kvService, userContext, dataset, stats);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            executor.submit(thread);
        }

        latch.countDown();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Run operation at port layer.
     */
    private void runOperationPort(Operation op, KVStorePort kvStorePort,
                                  HierarchicalDatasetGenerator.Dataset dataset, BenchmarkStatistics stats) {
        ExecutorService executor = Executors.newFixedThreadPool(config.threads());
        CountDownLatch latch = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < config.runs(); i++) {
            Thread thread = new Thread(() -> {
                try {
                    latch.await();
                    executeOperationPort(op, kvStorePort, dataset, stats);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            executor.submit(thread);
        }

        latch.countDown();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Execute operation at service layer.
     */
    private void executeOperation(Operation op, KVService kvService, UserContext userContext,
                                  HierarchicalDatasetGenerator.Dataset dataset, BenchmarkStatistics stats) {
        try {
            long startTime = System.nanoTime();
            switch (op) {
                case GET -> {
                    String relativePath = dataset.keys().get(ThreadLocalRandom.current().nextInt(dataset.keys().size()));
                    KVStorePort.KVReadOptions readOptions = buildReadOptions();
                    Optional<KVEntry> entry = kvService.get(config.serviceId(), relativePath, readOptions, userContext);
                    long endTime = System.nanoTime();
                    long latency = endTime - startTime;
                    if (entry.isPresent() && config.measureBytes()) {
                        stats.addLatency(latency, entry.get().value().length);
                    } else {
                        stats.addLatency(latency);
                    }
                }
                case PUT -> {
                    String relativePath = dataset.keys().get(ThreadLocalRandom.current().nextInt(dataset.keys().size()));
                    byte[] value = datasetGenerator.generateValue();
                    KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder().build();
                    kvService.put(config.serviceId(), relativePath, value, writeOptions, userContext);
                    long endTime = System.nanoTime();
                    stats.addLatency(endTime - startTime);
                }
                case DELETE -> {
                    String relativePath = dataset.keys().get(ThreadLocalRandom.current().nextInt(dataset.keys().size()));
                    KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder().build();
                    kvService.delete(config.serviceId(), relativePath, deleteOptions, userContext);
                    long endTime = System.nanoTime();
                    stats.addLatency(endTime - startTime);
                    // Restore for next run
                    try {
                        byte[] value = datasetGenerator.generateValue();
                        KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder().build();
                        kvService.put(config.serviceId(), relativePath, value, writeOptions, userContext);
                    } catch (Exception e) {
                        // Ignore restore errors
                    }
                }
                case LIST_KEYS -> {
                    String prefix = dataset.prefixes().get(ThreadLocalRandom.current().nextInt(dataset.prefixes().size()));
                    KVStorePort.KVListOptions listOptions = KVStorePort.KVListOptions.builder()
                            .keysOnly(true)
                            .recurse(true)
                            .build();
                    kvService.listKeys(config.serviceId(), prefix, listOptions, userContext);
                    long endTime = System.nanoTime();
                    stats.addLatency(endTime - startTime);
                }
                case LIST_RECURSE -> {
                    String prefix = dataset.prefixes().get(ThreadLocalRandom.current().nextInt(dataset.prefixes().size()));
                    KVStorePort.KVListOptions listOptions = KVStorePort.KVListOptions.builder()
                            .keysOnly(false)
                            .recurse(true)
                            .build();
                    List<KVEntry> entries = kvService.listEntries(config.serviceId(), prefix, listOptions, userContext);
                    long endTime = System.nanoTime();
                    long latency = endTime - startTime;
                    if (config.measureBytes()) {
                        long totalBytes = entries.stream().mapToLong(e -> e.value().length).sum();
                        stats.addLatency(latency, totalBytes);
                    } else {
                        stats.addLatency(latency);
                    }
                }
            }
        } catch (Exception e) {
            stats.incrementError();
            log.debug("Operation {} failed: {}", op, e.getMessage());
        }
    }

    /**
     * Execute operation at port layer.
     */
    private void executeOperationPort(Operation op, KVStorePort kvStorePort,
                                      HierarchicalDatasetGenerator.Dataset dataset, BenchmarkStatistics stats) {
        try {
            long startTime = System.nanoTime();
            switch (op) {
                case GET -> {
                    String relativePath = dataset.keys().get(ThreadLocalRandom.current().nextInt(dataset.keys().size()));
                    String absoluteKey = prefixPolicy.buildAbsoluteKey(config.serviceId(), relativePath);
                    KVStorePort.KVReadOptions readOptions = buildReadOptions();
                    Optional<KVEntry> entry = kvStorePort.get(absoluteKey, readOptions);
                    long endTime = System.nanoTime();
                    long latency = endTime - startTime;
                    if (entry.isPresent() && config.measureBytes()) {
                        stats.addLatency(latency, entry.get().value().length);
                    } else {
                        stats.addLatency(latency);
                    }
                }
                case PUT -> {
                    String relativePath = dataset.keys().get(ThreadLocalRandom.current().nextInt(dataset.keys().size()));
                    String absoluteKey = prefixPolicy.buildAbsoluteKey(config.serviceId(), relativePath);
                    byte[] value = datasetGenerator.generateValue();
                    KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder().build();
                    kvStorePort.put(absoluteKey, value, writeOptions);
                    long endTime = System.nanoTime();
                    stats.addLatency(endTime - startTime);
                }
                case DELETE -> {
                    String relativePath = dataset.keys().get(ThreadLocalRandom.current().nextInt(dataset.keys().size()));
                    String absoluteKey = prefixPolicy.buildAbsoluteKey(config.serviceId(), relativePath);
                    KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder().build();
                    kvStorePort.delete(absoluteKey, deleteOptions);
                    long endTime = System.nanoTime();
                    stats.addLatency(endTime - startTime);
                    // Restore for next run
                    try {
                        byte[] value = datasetGenerator.generateValue();
                        KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder().build();
                        kvStorePort.put(absoluteKey, value, writeOptions);
                    } catch (Exception e) {
                        // Ignore restore errors
                    }
                }
                case LIST_KEYS -> {
                    String prefix = dataset.prefixes().get(ThreadLocalRandom.current().nextInt(dataset.prefixes().size()));
                    String absolutePrefix = prefixPolicy.buildAbsolutePrefix(config.serviceId(), prefix);
                    KVStorePort.KVListOptions listOptions = KVStorePort.KVListOptions.builder()
                            .keysOnly(true)
                            .recurse(true)
                            .build();
                    kvStorePort.listKeys(absolutePrefix, listOptions);
                    long endTime = System.nanoTime();
                    stats.addLatency(endTime - startTime);
                }
                case LIST_RECURSE -> {
                    String prefix = dataset.prefixes().get(ThreadLocalRandom.current().nextInt(dataset.prefixes().size()));
                    String absolutePrefix = prefixPolicy.buildAbsolutePrefix(config.serviceId(), prefix);
                    KVStorePort.KVListOptions listOptions = KVStorePort.KVListOptions.builder()
                            .keysOnly(false)
                            .recurse(true)
                            .build();
                    List<KVEntry> entries = kvStorePort.listEntries(absolutePrefix, listOptions);
                    long endTime = System.nanoTime();
                    long latency = endTime - startTime;
                    if (config.measureBytes()) {
                        long totalBytes = entries.stream().mapToLong(e -> e.value().length).sum();
                        stats.addLatency(latency, totalBytes);
                    } else {
                        stats.addLatency(latency);
                    }
                }
            }
        } catch (Exception e) {
            stats.incrementError();
            log.debug("Operation {} failed: {}", op, e.getMessage());
        }
    }

    /**
     * Build read options from config.
     */
    private KVStorePort.KVReadOptions buildReadOptions() {
        KVStorePort.KVReadOptions.KVReadOptionsBuilder builder = KVStorePort.KVReadOptions.builder();
        switch (config.consistencyMode()) {
            case STALE -> builder.stale(true);
            case CONSISTENT -> builder.consistent(true);
            default -> {
                // DEFAULT - no flags
            }
        }
        return builder.raw(true).build();
    }
}

