package com.example.control.benchmark.kv;

import lombok.Builder;

import java.nio.file.Path;
import java.util.List;

/**
 * Configuration for KV benchmark execution.
 * <p>
 * Defines all parameters needed to run a benchmark: operations to test,
 * consistency modes, dataset shape, concurrency settings, and output paths.
 * </p>
 */
@Builder
public record BenchmarkConfig(
        /**
         * Operations to benchmark.
         */
        List<Operation> operations,

        /**
         * Consistency mode for read operations.
         */
        ConsistencyMode consistencyMode,

        /**
         * Base prefix for all generated keys.
         */
        String prefix,

        /**
         * Branching factor per level in hierarchical structure.
         */
        int fanout,

        /**
         * Number of hierarchical levels.
         */
        int depth,

        /**
         * Number of keys generated under each leaf node.
         */
        int keysPerLeaf,

        /**
         * Minimum key segment length.
         */
        int keyLenMin,

        /**
         * Maximum key segment length.
         */
        int keyLenMax,

        /**
         * Value size in bytes.
         */
        int valueSize,

        /**
         * Value generation mode: binary or text.
         */
        ValueMode valueMode,

        /**
         * Number of samples per operation.
         */
        int runs,

        /**
         * Number of warmup operations.
         */
        int warmup,

        /**
         * Number of concurrent threads.
         */
        int threads,

        /**
         * Output directory for reports.
         */
        Path outputDir,

        /**
         * Service ID for KVService benchmarks (required for service layer).
         */
        String serviceId,

        /**
         * Whether to measure payload size for GET/LIST operations.
         */
        boolean measureBytes
) {
    /**
     * Supported operations.
     */
    public enum Operation {
        GET,
        PUT,
        DELETE,
        LIST_KEYS,
        LIST_RECURSE
    }

    /**
     * Consistency modes for read operations.
     */
    public enum ConsistencyMode {
        DEFAULT,
        STALE,
        CONSISTENT
    }

    /**
     * Value generation modes.
     */
    public enum ValueMode {
        BINARY,
        TEXT
    }

    /**
     * Create default configuration.
     */
    public static BenchmarkConfig defaults() {
        return BenchmarkConfig.builder()
                .operations(List.of(Operation.GET, Operation.PUT, Operation.DELETE, Operation.LIST_KEYS, Operation.LIST_RECURSE))
                .consistencyMode(ConsistencyMode.DEFAULT)
                .prefix("bench/kv")
                .fanout(8)
                .depth(3)
                .keysPerLeaf(4)
                .keyLenMin(8)
                .keyLenMax(16)
                .valueSize(512)
                .valueMode(ValueMode.BINARY)
                .runs(2000)
                .warmup(300)
                .threads(8)
                .outputDir(Path.of("build/benchmark-results"))
                .serviceId("benchmark-service")
                .measureBytes(false)
                .build();
    }

    /**
     * Create configuration from system properties.
     */
    public static BenchmarkConfig fromSystemProperties() {
        BenchmarkConfig defaults = defaults();
        BenchmarkConfig.BenchmarkConfigBuilder builder = defaults.toBuilder();

        // Parse operations
        String opsProp = System.getProperty("benchmark.ops");
        if (opsProp != null && !opsProp.isBlank()) {
            List<Operation> ops = java.util.Arrays.stream(opsProp.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(Operation::valueOf)
                    .toList();
            builder.operations(ops);
        }

        // Parse consistency mode
        String consistencyProp = System.getProperty("benchmark.consistency");
        if (consistencyProp != null && !consistencyProp.isBlank()) {
            builder.consistencyMode(ConsistencyMode.valueOf(consistencyProp.toUpperCase()));
        }

        // Parse dataset parameters
        String prefixProp = System.getProperty("benchmark.prefix");
        if (prefixProp != null && !prefixProp.isBlank()) {
            builder.prefix(prefixProp);
        }

        String fanoutProp = System.getProperty("benchmark.fanout");
        if (fanoutProp != null && !fanoutProp.isBlank()) {
            builder.fanout(Integer.parseInt(fanoutProp));
        }

        String depthProp = System.getProperty("benchmark.depth");
        if (depthProp != null && !depthProp.isBlank()) {
            builder.depth(Integer.parseInt(depthProp));
        }

        String keysPerLeafProp = System.getProperty("benchmark.keysPerLeaf");
        if (keysPerLeafProp != null && !keysPerLeafProp.isBlank()) {
            builder.keysPerLeaf(Integer.parseInt(keysPerLeafProp));
        }

        String valueSizeProp = System.getProperty("benchmark.valueSize");
        if (valueSizeProp != null && !valueSizeProp.isBlank()) {
            builder.valueSize(Integer.parseInt(valueSizeProp));
        }

        // Parse concurrency parameters
        String runsProp = System.getProperty("benchmark.runs");
        if (runsProp != null && !runsProp.isBlank()) {
            builder.runs(Integer.parseInt(runsProp));
        }

        String threadsProp = System.getProperty("benchmark.threads");
        if (threadsProp != null && !threadsProp.isBlank()) {
            builder.threads(Integer.parseInt(threadsProp));
        }

        String warmupProp = System.getProperty("benchmark.warmup");
        if (warmupProp != null && !warmupProp.isBlank()) {
            builder.warmup(Integer.parseInt(warmupProp));
        }

        // Parse output directory
        String outputDirProp = System.getProperty("benchmark.outputDir");
        if (outputDirProp != null && !outputDirProp.isBlank()) {
            builder.outputDir(Path.of(outputDirProp));
        }

        // Parse service ID
        String serviceIdProp = System.getProperty("benchmark.serviceId");
        if (serviceIdProp != null && !serviceIdProp.isBlank()) {
            builder.serviceId(serviceIdProp);
        }

        // Parse measure bytes
        String measureBytesProp = System.getProperty("benchmark.measureBytes");
        if (measureBytesProp != null && !measureBytesProp.isBlank()) {
            builder.measureBytes(Boolean.parseBoolean(measureBytesProp));
        }

        return builder.build();
    }

    /**
     * Convert to builder for modification.
     */
    public BenchmarkConfigBuilder toBuilder() {
        return BenchmarkConfig.builder()
                .operations(operations)
                .consistencyMode(consistencyMode)
                .prefix(prefix)
                .fanout(fanout)
                .depth(depth)
                .keysPerLeaf(keysPerLeaf)
                .keyLenMin(keyLenMin)
                .keyLenMax(keyLenMax)
                .valueSize(valueSize)
                .valueMode(valueMode)
                .runs(runs)
                .warmup(warmup)
                .threads(threads)
                .outputDir(outputDir)
                .serviceId(serviceId)
                .measureBytes(measureBytes);
    }
}

