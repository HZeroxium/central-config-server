package com.example.control.benchmark.kv.reporter;

import com.example.control.benchmark.kv.BenchmarkStatistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports benchmark statistics to CSV file.
 */
public class CSVReporter {

    /**
     * Write summary to CSV file.
     *
     * @param summaries list of operation summaries
     * @param totalTimeSeconds total benchmark time
     * @param outputPath output file path
     * @throws IOException if file write fails
     */
    public void writeSummary(List<BenchmarkStatistics.Summary> summaries, double totalTimeSeconds, Path outputPath) throws IOException {
        List<String> lines = new ArrayList<>();

        // Header
        lines.add("operation,count,min_ms,p50_ms,avg_ms,p90_ms,p95_ms,p99_ms,max_ms,std_ms,ops_per_sec,bytes_total,error_count");

        // Data rows
        for (BenchmarkStatistics.Summary summary : summaries) {
            double throughput = summary.calculateThroughput(totalTimeSeconds);
            lines.add(String.format("%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%d,%d",
                    summary.operationName(),
                    summary.count(),
                    summary.toMs(summary.minNanos()),
                    summary.toMs(summary.p50Nanos()),
                    summary.toMs(summary.avgNanos()),
                    summary.toMs(summary.p90Nanos()),
                    summary.toMs(summary.p95Nanos()),
                    summary.toMs(summary.p99Nanos()),
                    summary.toMs(summary.maxNanos()),
                    summary.toMs(summary.stddevNanos()),
                    throughput,
                    summary.bytesTotal(),
                    summary.errorCount()));
        }

        // Ensure parent directory exists
        Files.createDirectories(outputPath.getParent());

        // Write file
        Files.write(outputPath, lines);
    }
}

