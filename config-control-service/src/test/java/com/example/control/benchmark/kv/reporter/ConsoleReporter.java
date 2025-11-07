package com.example.control.benchmark.kv.reporter;

import com.example.control.benchmark.kv.BenchmarkStatistics;

import java.io.PrintStream;
import java.util.List;

/**
 * Formats benchmark statistics as human-readable console output.
 */
public class ConsoleReporter {

    private final PrintStream out;

    public ConsoleReporter() {
        this(System.out);
    }

    public ConsoleReporter(PrintStream out) {
        this.out = out;
    }

    /**
     * Print summary table.
     *
     * @param summaries list of operation summaries
     * @param totalTimeSeconds total benchmark time
     */
    public void printSummary(List<BenchmarkStatistics.Summary> summaries, double totalTimeSeconds) {
        out.println("\n=== KV Benchmark Results ===");
        out.println(String.format("Total time: %.3f seconds", totalTimeSeconds));
        out.println();

        // Print header
        out.printf("%-20s %8s %10s %10s %10s %10s %10s %10s %10s %10s %12s%n",
                "Operation", "Count", "Min(ms)", "P50", "Avg", "P90", "P95", "P99", "Max(ms)", "Std(ms)", "Ops/sec");

        // Print separator
        out.println("-".repeat(120));

        // Print data rows
        for (BenchmarkStatistics.Summary summary : summaries) {
            double throughput = summary.calculateThroughput(totalTimeSeconds);
            out.printf("%-20s %8d %10.3f %10.3f %10.3f %10.3f %10.3f %10.3f %10.3f %10.3f %12.2f%n",
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
                    throughput);

            if (summary.errorCount() > 0) {
                out.printf("  └─ Errors: %d%n", summary.errorCount());
            }
        }

        out.println();
    }
}

