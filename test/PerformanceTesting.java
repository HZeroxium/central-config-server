import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PerformanceTesting {

    // Configurable parameters
    private static String METHOD = "GET";            // "GET" or "POST"
    private static String ENDPOINT = "http://localhost:8083/users";
    private static String POST_BODY = "{\"name\":\"test\"}"; // for POST
    private static int CONNECT_TIMEOUT_MS = 5000;
    private static int READ_TIMEOUT_MS = 5000;

    // Scenario mode: choose one of "LOAD", "STRESS", "SPIKE"
    private static String TEST_MODE = "LOAD";  

    // Base load parameters (for LOAD scenario)
    private static int LOAD_THREADS = 100;             // number of concurrent threads for LOAD
    private static int LOAD_REQUESTS_PER_THREAD = 150; // per thread
    private static long LOAD_RAMP_UP_MS = 20000;       // ramp-up period for LOAD in ms
    private static long LOAD_HOLD_DURATION_MS = 60000;  // how long to hold full load

    // Stress test parameters
    private static int STRESS_MAX_THREADS = 150;          // maximum threads to try
    private static int STRESS_INCREMENT_THREADS = 25;     // how many threads to increase per step
    private static long STRESS_STEP_DURATION_MS = 30000;   // how long to hold each step
    private static long STRESS_RAMP_UP_MS = 5000;          // ramp-up for each step

    // Spike test parameters
    private static int SPIKE_BASE_THREADS = 20;            // baseline threads
    private static int SPIKE_SPIKE_THREADS = 200;          // threads during spike
    private static long SPIKE_BASE_DURATION_MS = 60000;    // baseline duration
    private static long SPIKE_RAMP_UP_MS = 2000;           // ramp-up into spike
    private static long SPIKE_SPIKE_DURATION_MS = 20000;   // time at spike
    private static long SPIKE_COOLDOWN_MS = 15000;         // after spike, cooldown

    public static void main(String[] args) throws Exception {
        // Optionally override via args
        // e.g. args[0] = TEST_MODE (LOAD/STRESS/SPIKE)
        if (args.length >= 1) TEST_MODE = args[0];
        if (args.length >= 2) ENDPOINT = args[1];

        System.out.println("=== Performance Testing Scenario ===");
        System.out.println("Test mode: " + TEST_MODE);
        System.out.println("Endpoint: " + ENDPOINT);

        switch (TEST_MODE.toUpperCase()) {
            case "LOAD":
                runLoadTest();
                break;
            case "STRESS":
                runStressTest();
                break;
            case "SPIKE":
                runSpikeTest();
                break;
            default:
                System.err.println("Unknown TEST_MODE: " + TEST_MODE);
                System.err.println("Use LOAD, STRESS, or SPIKE");
                System.exit(1);
        }
    }

    private static void runLoadTest() throws InterruptedException {
        System.out.println("Starting LOAD test");
        System.out.println("Threads: " + LOAD_THREADS + ", Requests/thread: " + LOAD_REQUESTS_PER_THREAD
            + ", Ramp-up(ms): " + LOAD_RAMP_UP_MS + ", Hold full load for(ms): " + LOAD_HOLD_DURATION_MS);

        // Ramp-up: gradually start threads over LOAD_RAMP_UP_MS
        long delayBetweenThreads = LOAD_RAMP_UP_MS / LOAD_THREADS;

        ExecutorService executor = Executors.newFixedThreadPool(LOAD_THREADS);

        for (int i = 0; i < LOAD_THREADS; i++) {
            final int threadNum = i + 1;
            executor.submit(() -> {
                for (int j = 0; j < LOAD_REQUESTS_PER_THREAD; j++) {
                    try {
                        long start = System.currentTimeMillis();
                        int status = sendRequest();
                        long elapsed = System.currentTimeMillis() - start;
                        System.out.println("LOAD Thread-" + threadNum
                            + " Request#" + j + " => status: " + status
                            + " time(ms): " + elapsed);
                    } catch (Exception e) {
                        System.err.println("LOAD Thread-" + threadNum + " failed: " + e.getMessage());
                    }
                }
            });
            // Ramp up delay
            if (delayBetweenThreads > 0) {
                Thread.sleep(delayBetweenThreads);
            }
        }

        // Hold full load for a period so threads can continue sending
        Thread.sleep(LOAD_HOLD_DURATION_MS);

        executor.shutdown();
        boolean finished = executor.awaitTermination(1, TimeUnit.HOURS);
        if (finished) {
            System.out.println("LOAD test completed");
        } else {
            System.out.println("LOAD test did NOT complete within timeout");
        }
    }

    private static void runStressTest() throws InterruptedException {
        System.out.println("Starting STRESS test");
        System.out.println("Max threads: " + STRESS_MAX_THREADS
            + ", increment: " + STRESS_INCREMENT_THREADS
            + ", step duration(ms): " + STRESS_STEP_DURATION_MS
            + ", ramp-up per step(ms): " + STRESS_RAMP_UP_MS);

        // We'll increase load by steps until reaching STRESS_MAX_THREADS
        for (int currentThreads = STRESS_INCREMENT_THREADS;
             currentThreads <= STRESS_MAX_THREADS;
             currentThreads += STRESS_INCREMENT_THREADS) {

            System.out.println("--- Stress Step: threads = " + currentThreads);

            long delayBetweenThreads = STRESS_RAMP_UP_MS / currentThreads;
            ExecutorService executor = Executors.newFixedThreadPool(currentThreads);

            for (int i = 0; i < currentThreads; i++) {
                final int threadNum = i + 1;
                executor.submit(() -> {
                    // In stress, for each step we send requests continuously during step duration
                    long stepEndTime = System.currentTimeMillis() + STRESS_STEP_DURATION_MS;
                    int count = 0;
                    while (System.currentTimeMillis() < stepEndTime) {
                        try {
                            long start = System.currentTimeMillis();
                            int status = sendRequest();
                            long elapsed = System.currentTimeMillis() - start;
                            System.out.println("STRESS Thread-" + threadNum
                                + " Req#" + count + " => status: " + status
                                + " time(ms): " + elapsed);
                            count++;
                        } catch (Exception e) {
                            System.err.println("STRESS Thread-" + threadNum
                                + " failed: " + e.getMessage());
                        }
                    }
                });
                if (delayBetweenThreads > 0) {
                    Thread.sleep(delayBetweenThreads);
                }
            }

            // Wait until this step finishes
            executor.shutdown();
            boolean finished = executor.awaitTermination( STRESS_STEP_DURATION_MS + 60_000, TimeUnit.MILLISECONDS);
            if (!finished) {
                System.out.println("Stress step with " + currentThreads + " threads did NOT complete in time");
            } else {
                System.out.println("Stress step " + currentThreads + " threads completed");
            }

            // Optionally small pause between steps
            Thread.sleep(2000);
        }

        System.out.println("STRESS test completed");
    }

    private static void runSpikeTest() throws InterruptedException {
        System.out.println("Starting SPIKE test");
        System.out.println("Baseline threads: " + SPIKE_BASE_THREADS
            + ", baseline duration(ms): " + SPIKE_BASE_DURATION_MS
            + ", spike threads: " + SPIKE_SPIKE_THREADS
            + ", ramp-up(ms) into spike: " + SPIKE_RAMP_UP_MS
            + ", spike duration(ms): " + SPIKE_SPIKE_DURATION_MS
            + ", cooldown(ms): " + SPIKE_COOLDOWN_MS);

        // Phase 1: baseline load
        runSimplePhase(SPIKE_BASE_THREADS, SPIKE_BASE_DURATION_MS, 0);

        // Phase 2: ramp-up into spike
        System.out.println("Ramp-up into SPIKE");
        runSimplePhase(SPIKE_SPIKE_THREADS, SPIKE_SPIKE_DURATION_MS, SPIKE_RAMP_UP_MS);

        // Phase 3: cooldown (return to baseline or zero)
        System.out.println("Cooldown after SPIKE");
        runSimplePhase(SPIKE_BASE_THREADS, SPIKE_COOLDOWN_MS, SPIKE_RAMP_UP_MS);

        System.out.println("SPIKE test completed");
    }

    /**
     * Helper method to run a simple phase:
     *   - number of threads = threadCount
     *   - duration total = durationMs
     *   - ramp-up up to threadCount over rampUpMs (if rampUpMs > 0)
     *
     * All threads will continuously send requests until the phase duration ends.
     */
    private static void runSimplePhase(int threadCount, long durationMs, long rampUpMs) throws InterruptedException {
        System.out.println("Phase: threads=" + threadCount + ", duration(ms)=" + durationMs + ", rampUp(ms)=" + rampUpMs);

        long delayBetweenThreads = (rampUpMs > 0 && threadCount > 0) ? rampUpMs / threadCount : 0;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        long phaseEndTime = System.currentTimeMillis() + durationMs;

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i + 1;
            executor.submit(() -> {
                int count = 0;
                while (System.currentTimeMillis() < phaseEndTime) {
                    try {
                        long start = System.currentTimeMillis();
                        int status = sendRequest();
                        long elapsed = System.currentTimeMillis() - start;
                        System.out.println("Phase Thread-" + threadNum
                            + " Req#" + count + " => status: " + status
                            + " time(ms): " + elapsed);
                        count++;
                    } catch (Exception e) {
                        System.err.println("Phase Thread-" + threadNum + " failed: " + e.getMessage());
                    }
                }
            });
            if (delayBetweenThreads > 0) {
                Thread.sleep(delayBetweenThreads);
            }
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(durationMs + 60_000, TimeUnit.MILLISECONDS);
        if (!finished) {
            System.out.println("Phase threads did NOT complete in allocated time");
        } else {
            System.out.println("Phase completed");
        }
    }

    private static int sendRequest() throws Exception {
        URL url = new URL(ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(METHOD);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);

        if ("POST".equalsIgnoreCase(METHOD)) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(POST_BODY.getBytes("UTF-8"));
                os.flush();
            }
        }

        int status = conn.getResponseCode();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream()
                ))) {
            String line;
            while ((line = in.readLine()) != null) {
                // read and discard
            }
        }
        conn.disconnect();
        return status;
    }
}
