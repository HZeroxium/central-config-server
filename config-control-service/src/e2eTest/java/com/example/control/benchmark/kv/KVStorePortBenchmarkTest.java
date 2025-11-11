package com.example.control.benchmark.kv;

import com.example.control.benchmark.kv.reporter.ConsoleReporter;
import com.example.control.benchmark.kv.reporter.CSVReporter;
import com.example.control.benchmark.kv.reporter.JSONReporter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Benchmark tests for KVStorePort layer (direct adapter).
 * <p>
 * Measures performance of KV operations at the port layer, bypassing
 * permission checks and business logic to measure raw Consul adapter performance.
 * </p>
 */
@Slf4j
@DisplayName("KVStorePort Layer Benchmark")
public class KVStorePortBenchmarkTest extends BaseKVBenchmarkTest {

    @Test
    @DisplayName("Benchmark KVStorePort operations")
    void benchmarkKVStorePort() throws Exception {
        // Check if this layer should be benchmarked
        String layer = System.getProperty("benchmark.layer", "both");
        if (!"both".equals(layer) && !"port".equals(layer)) {
            log.info("Skipping KVStorePort benchmark (layer={})", layer);
            return;
        }

        log.info("=== Starting KVStorePort Benchmark ===");
        log.info("Config: {}", config);

        // Run benchmark
        long startTime = System.nanoTime();
        List<BenchmarkStatistics.Summary> summaries = benchmarkRunner.runPortBenchmark(kvStorePort);
        long endTime = System.nanoTime();
        double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;

        // Validate results
        assertNotNull(summaries, "Summaries should not be null");
        assertFalse(summaries.isEmpty(), "Summaries should not be empty");

        // Print console output
        ConsoleReporter consoleReporter = new ConsoleReporter();
        consoleReporter.printSummary(summaries, totalTimeSeconds);

        // Write CSV report
        Path csvPath = config.outputDir().resolve("kvstoreport-benchmark-" + Instant.now().toEpochMilli() + ".csv");
        CSVReporter csvReporter = new CSVReporter();
        csvReporter.writeSummary(summaries, totalTimeSeconds, csvPath);
        log.info("CSV report written: {}", csvPath.toAbsolutePath());

        // Write JSON report
        Path jsonPath = config.outputDir().resolve("kvstoreport-benchmark-" + Instant.now().toEpochMilli() + ".json");
        JSONReporter jsonReporter = new JSONReporter();
        jsonReporter.writeSummary(config, summaries, totalTimeSeconds, "KVStorePort", jsonPath);
        log.info("JSON report written: {}", jsonPath.toAbsolutePath());

        log.info("=== KVStorePort Benchmark Completed ===");
    }
}

