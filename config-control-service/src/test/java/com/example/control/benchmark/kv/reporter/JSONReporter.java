package com.example.control.benchmark.kv.reporter;

import com.example.control.benchmark.kv.BenchmarkConfig;
import com.example.control.benchmark.kv.BenchmarkStatistics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports benchmark statistics to JSON file with full metadata.
 */
public class JSONReporter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Write summary to JSON file.
     *
     * @param config benchmark configuration
     * @param summaries list of operation summaries
     * @param totalTimeSeconds total benchmark time
     * @param layer benchmark layer (KVService or KVStorePort)
     * @param outputPath output file path
     * @throws IOException if file write fails
     */
    public void writeSummary(BenchmarkConfig config, List<BenchmarkStatistics.Summary> summaries,
                             double totalTimeSeconds, String layer, Path outputPath) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode benchmark = root.putObject("benchmark");

        // Add configuration
        ObjectNode configNode = benchmark.putObject("config");
        configNode.put("prefix", config.prefix());
        configNode.put("fanout", config.fanout());
        configNode.put("depth", config.depth());
        configNode.put("keysPerLeaf", config.keysPerLeaf());
        configNode.put("valueSize", config.valueSize());
        configNode.put("valueMode", config.valueMode().name());
        configNode.put("runs", config.runs());
        configNode.put("threads", config.threads());
        configNode.put("warmup", config.warmup());
        configNode.put("consistencyMode", config.consistencyMode().name());
        configNode.put("layer", layer);

        // Add results
        ArrayNode results = benchmark.putArray("results");
        for (BenchmarkStatistics.Summary summary : summaries) {
            ObjectNode result = results.addObject();
            result.put("operation", summary.operationName());
            result.put("layer", layer);
            result.put("count", summary.count());
            result.put("errorCount", summary.errorCount());

            // Latency metrics
            ObjectNode latency = result.putObject("latency");
            latency.put("min_ms", summary.toMs(summary.minNanos()));
            latency.put("p50_ms", summary.toMs(summary.p50Nanos()));
            latency.put("avg_ms", summary.toMs(summary.avgNanos()));
            latency.put("p90_ms", summary.toMs(summary.p90Nanos()));
            latency.put("p95_ms", summary.toMs(summary.p95Nanos()));
            latency.put("p99_ms", summary.toMs(summary.p99Nanos()));
            latency.put("max_ms", summary.toMs(summary.maxNanos()));
            latency.put("std_ms", summary.toMs(summary.stddevNanos()));

            // Throughput
            ObjectNode throughput = result.putObject("throughput");
            throughput.put("ops_per_sec", summary.calculateThroughput(totalTimeSeconds));

            // Payload size
            if (summary.bytesTotal() > 0) {
                result.put("bytesTotal", summary.bytesTotal());
            }
        }

        // Add metadata
        ObjectNode metadata = benchmark.putObject("metadata");
        metadata.put("totalTimeSeconds", totalTimeSeconds);
        metadata.put("timestamp", java.time.Instant.now().toString());

        // Ensure parent directory exists
        Files.createDirectories(outputPath.getParent());

        // Write file
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), root);
    }
}

