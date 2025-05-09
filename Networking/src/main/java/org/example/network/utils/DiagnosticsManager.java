package org.example.network.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for application diagnostics including performance monitoring
 * and network traffic analysis.
 */
public class DiagnosticsManager {
    private static final Logger logger = LogManager.getLogger(DiagnosticsManager.class);
    private static final Map<String, Long> operationTimings = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> networkTrafficInBytes = new ConcurrentHashMap<>();

    // Prevent instantiation
    private DiagnosticsManager() {}

    /**
     * Start timing an operation
     */
    public static void startTiming(String operation) {
        operationTimings.put(operation, System.currentTimeMillis());
    }

    /**
     * End timing an operation and log the result
     */
    public static void endTiming(String operation) {
        Long startTime = operationTimings.remove(operation);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Operation '{}' took {} ms", operation, duration);

            // Record operation count
            operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0))
                    .incrementAndGet();
        }
    }

    /**
     * Log network traffic statistics
     */
    public static void logNetworkStats(String requestType, int requestSize, int responseSize) {
        logger.debug("Network traffic - {} request: {} bytes, response: {} bytes",
                requestType, requestSize, responseSize);

        // Track total network traffic
        networkTrafficInBytes.computeIfAbsent("requests", k -> new AtomicLong(0))
                .addAndGet(requestSize);
        networkTrafficInBytes.computeIfAbsent("responses", k -> new AtomicLong(0))
                .addAndGet(responseSize);
        networkTrafficInBytes.computeIfAbsent(requestType + "_requests", k -> new AtomicLong(0))
                .addAndGet(requestSize);
        networkTrafficInBytes.computeIfAbsent(requestType + "_responses", k -> new AtomicLong(0))
                .addAndGet(responseSize);
    }

    /**
     * Log UI update statistics
     */
    public static void logUIUpdate(String controllerName, String updateType, long processingTime) {
        logger.debug("UI Update - Controller: {}, Type: {}, Time: {} ms",
                controllerName, updateType, processingTime);

        // Record update counts
        String key = controllerName + "." + updateType;
        operationCounts.computeIfAbsent(key, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    /**
     * Generate diagnostics report
     */
    public static String generateReport() {
        StringBuilder report = new StringBuilder("Diagnostics Report\n");
        report.append("===================\n\n");

        report.append("Operation Counts:\n");
        operationCounts.forEach((op, count) ->
                report.append(String.format("  %-30s %d\n", op, count.get())));

        report.append("\nActive Timings:\n");
        operationTimings.forEach((op, startTime) -> {
            long duration = System.currentTimeMillis() - startTime;
            report.append(String.format("  %-30s %d ms (ongoing)\n", op, duration));
        });

        report.append("\nNetwork Traffic:\n");
        networkTrafficInBytes.forEach((type, bytes) ->
                report.append(String.format("  %-30s %d bytes\n", type, bytes.get())));

        return report.toString();
    }

    /**
     * Reset all statistics
     */
    public static void resetStats() {
        operationCounts.clear();
        operationTimings.clear();
        networkTrafficInBytes.clear();
        logger.info("Diagnostics statistics reset");
    }

    /**
     * Log connection event
     */
    public static void logConnectionEvent(String status, String host, int port) {
        logger.info("Connection {} to {}:{}", status, host, port);

        String key = "connection_" + status.toLowerCase();
        operationCounts.computeIfAbsent(key, k -> new AtomicLong(0))
                .incrementAndGet();
    }
}