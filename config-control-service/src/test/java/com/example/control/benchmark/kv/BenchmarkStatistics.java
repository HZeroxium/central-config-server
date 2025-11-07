package com.example.control.benchmark.kv;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects and calculates benchmark statistics.
 * <p>
 * Tracks latency samples per operation and calculates percentiles,
 * standard deviation, and throughput metrics.
 * </p>
 */
@Data
public class BenchmarkStatistics {

    private final String operationName;
    private final List<Long> latencies = new ArrayList<>();
    private long bytesTotal = 0;
    private int errorCount = 0;

    public BenchmarkStatistics(String operationName) {
        this.operationName = operationName;
    }

    /**
     * Add a latency sample.
     *
     * @param nanos latency in nanoseconds
     */
    public void addLatency(long nanos) {
        latencies.add(nanos);
    }

    /**
     * Add a latency sample with payload size.
     *
     * @param nanos latency in nanoseconds
     * @param bytes payload size in bytes
     */
    public void addLatency(long nanos, long bytes) {
        latencies.add(nanos);
        bytesTotal += bytes;
    }

    /**
     * Increment error count.
     */
    public void incrementError() {
        errorCount++;
    }

    /**
     * Calculate summary statistics.
     *
     * @return summary with all metrics
     */
    public Summary summarize() {
        if (latencies.isEmpty()) {
            return new Summary(
                    operationName,
                    0,
                    0L,
                    0L,
                    0.0,
                    0L,
                    0L,
                    0L,
                    0L,
                    0.0,
                    0L,
                    0
            );
        }

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        int n = sorted.size();
        long min = sorted.get(0);
        long max = sorted.get(n - 1);
        double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = percentile(sorted, 0.50);
        long p90 = percentile(sorted, 0.90);
        long p95 = percentile(sorted, 0.95);
        long p99 = percentile(sorted, 0.99);
        double stddev = calculateStdDev(sorted, avg);

        return new Summary(
                operationName,
                n,
                min,
                max,
                avg,
                p50,
                p90,
                p95,
                p99,
                stddev,
                bytesTotal,
                errorCount
        );
    }

    /**
     * Calculate percentile.
     *
     * @param sorted sorted list of values
     * @param p      percentile (0.0 to 1.0)
     * @return percentile value
     */
    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int idx = (int) Math.ceil(p * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }

    /**
     * Calculate standard deviation.
     *
     * @param sorted sorted list of values
     * @param avg    average value
     * @return standard deviation
     */
    private static double calculateStdDev(List<Long> sorted, double avg) {
        if (sorted.size() < 2) {
            return 0;
        }
        double variance = 0;
        for (long v : sorted) {
            double d = v - avg;
            variance += d * d;
        }
        variance /= (sorted.size() - 1);
        return Math.sqrt(variance);
    }

    /**
     * Summary statistics.
     */
    public record Summary(
            String operationName,
            int count,
            long minNanos,
            long maxNanos,
            double avgNanos,
            long p50Nanos,
            long p90Nanos,
            long p95Nanos,
            long p99Nanos,
            double stddevNanos,
            long bytesTotal,
            int errorCount
    ) {
        /**
         * Convert nanoseconds to milliseconds.
         */
        public double toMs(long nanos) {
            return nanos / 1_000_000.0;
        }

        /**
         * Convert nanoseconds to milliseconds (for double values).
         */
        public double toMs(double nanos) {
            return nanos / 1_000_000.0;
        }

        /**
         * Calculate throughput (ops/sec).
         *
         * @param totalTimeSeconds total benchmark time in seconds
         * @return operations per second
         */
        public double calculateThroughput(double totalTimeSeconds) {
            if (totalTimeSeconds <= 0) {
                return 0;
            }
            return count / totalTimeSeconds;
        }
    }
}

