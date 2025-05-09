package org.example.network.rpc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.network.dto.MatchDTO;
import org.example.network.dto.TicketDTO;
import org.example.network.dto.UserDTO;
import org.example.network.protocol.Response;
import org.example.network.protocol.ResponseType;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Singleton class that manages broadcasting updates to connected clients
 * with batching to reduce update frequency
 */
public class UpdateBroadcaster {
    private static final Logger logger = LogManager.getLogger(UpdateBroadcaster.class);
    private static UpdateBroadcaster instance;

    // Thread-safe collection for storing listeners
    private final List<BasketballJsonWorker> listeners = new CopyOnWriteArrayList<>();

    // Thread pool for async update delivery
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // Batching mechanism
    private final Map<String, Response> pendingUpdates = new ConcurrentHashMap<>();
    private final ScheduledExecutorService batchScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean batchScheduled = new AtomicBoolean(false);

    // Default batching delay in milliseconds
    private static final long DEFAULT_BATCH_DELAY = 100;
    private static final long QUICK_BATCH_DELAY = 50;

    private UpdateBroadcaster() {
        logger.info("UpdateBroadcaster singleton created with batching enabled");
    }

    public static synchronized UpdateBroadcaster getInstance() {
        if (instance == null) {
            instance = new UpdateBroadcaster();
        }
        return instance;
    }

    /**
     * Registers a listener to receive updates
     */
    public synchronized void registerListener(BasketballJsonWorker listener) {
        if (listener == null) {
            logger.warn("Attempted to register null listener");
            return;
        }

        if (!listeners.contains(listener)) {
            listeners.add(listener);
            logger.info("Update listener registered - total count: {}", listeners.size());
        } else {
            logger.warn("Listener already registered");
        }
    }

    /**
     * Unregisters a listener
     */
    public synchronized void unregisterListener(BasketballJsonWorker listener) {
        if (listener == null) {
            logger.warn("Attempted to unregister null listener");
            return;
        }

        boolean removed = listeners.remove(listener);
        if (removed) {
            logger.info("Update listener unregistered - remaining count: {}", listeners.size());
        } else {
            logger.warn("Attempted to unregister listener that wasn't registered");
        }
    }

    /**
     * Buffers an update for batched delivery to all connected listeners
     */
    public void broadcastUpdate(Response update) {
        if (update == null) {
            logger.warn("Attempted to broadcast null update");
            return;
        }

        int listenerCount = listeners.size();
        if (listenerCount == 0) {
            logger.warn("No listeners registered for update: {}", update.getType());
            return;
        }

        // Buffer the update with a key based on update type and entity ID
        String key = getUpdateKey(update);
        logger.debug("Buffering update of type {} with key {}", update.getType(), key);
        pendingUpdates.put(key, update);

        // Schedule batch delivery if not already scheduled
        if (batchScheduled.compareAndSet(false, true)) {
            logger.debug("Scheduling batch update delivery in {} ms", DEFAULT_BATCH_DELAY);
            batchScheduler.schedule(this::deliverPendingUpdates, DEFAULT_BATCH_DELAY, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Processes and delivers all pending updates in a batch
     */
    private void deliverPendingUpdates() {
        try {
            // Create a snapshot of current updates to process
            Map<String, Response> updates = new HashMap<>(pendingUpdates);
            pendingUpdates.clear();

            if (updates.isEmpty()) {
                logger.debug("No updates to deliver in this batch");
                return;
            }

            logger.info("Processing batch of {} updates", updates.size());

            // Group updates by type to consolidate them
            Map<ResponseType, List<Response>> groupedUpdates = updates.values().stream()
                    .collect(Collectors.groupingBy(Response::getType));

            // Process each update type
            for (Map.Entry<ResponseType, List<Response>> entry : groupedUpdates.entrySet()) {
                ResponseType type = entry.getKey();
                List<Response> typeUpdates = entry.getValue();

                logger.debug("Delivering {} updates of type {}", typeUpdates.size(), type);

                // For each type, only send the most recent update
                Response latestUpdate = typeUpdates.get(typeUpdates.size() - 1);
                doActualBroadcast(latestUpdate);
            }

            logger.debug("Batch update delivery completed");
        } catch (Exception e) {
            logger.error("Error in batch update delivery", e);
        } finally {
            batchScheduled.set(false);

            // If new updates arrived during processing, schedule another batch
            if (!pendingUpdates.isEmpty()) {
                if (batchScheduled.compareAndSet(false, true)) {
                    logger.debug("New updates arrived during processing, scheduling quick follow-up batch");
                    batchScheduler.schedule(this::deliverPendingUpdates, QUICK_BATCH_DELAY, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private String getUpdateKey(Response update) {
        ResponseType type = update.getType();
        Object data = update.getData();

        // Special handling for consolidated updates
        if (type == ResponseType.CONSOLIDATED_UPDATE && data instanceof Map) {
            Map<String, Object> consolidatedData = (Map<String, Object>) data;
            String operation = (String) consolidatedData.getOrDefault("operation", "UNKNOWN");

            if (consolidatedData.containsKey("match")) {
                Object matchData = consolidatedData.get("match");
                // Handle both Map and MatchDTO cases
                String matchId = extractId(matchData);
                if (matchId != null) {
                    return "CONSOLIDATED:" + operation + ":MATCH:" + matchId;
                }
            }

            if (consolidatedData.containsKey("user")) {
                Object userData = consolidatedData.get("user");
                String userId = extractId(userData);
                if (userId != null) {
                    return "CONSOLIDATED:" + operation + ":USER:" + userId;
                }
            }

            return "CONSOLIDATED:" + operation;
        }

        // Handle DTO objects directly
        if (data instanceof MatchDTO) {
            MatchDTO matchDTO = (MatchDTO) data;
            return type + ":MATCH:" + matchDTO.getId();
        } else if (data instanceof UserDTO) {
            UserDTO userDTO = (UserDTO) data;
            return type + ":USER:" + userDTO.getId();
        } else if (data instanceof TicketDTO) {
            TicketDTO ticketDTO = (TicketDTO) data;
            return type + ":TICKET:" + ticketDTO.getId();
        }

        // Handle entity-specific updates in Map format
        else if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;

            // For standard entity updates
            if (dataMap.containsKey("id")) {
                return type + ":" + dataMap.get("id");
            }

            // For match-related updates
            if (dataMap.containsKey("matchId")) {
                return type + ":MATCH:" + dataMap.get("matchId");
            }

            // For user-related updates
            if (dataMap.containsKey("userId")) {
                return type + ":USER:" + dataMap.get("userId");
            }
        }

        // Fallback to just the update type
        return type.toString();
    }

    /**
     * Helper method to extract ID from different object types
     */
    private String extractId(Object data) {
        if (data instanceof Map) {
            return String.valueOf(((Map<String, Object>) data).get("id"));
        } else if (data instanceof MatchDTO) {
            return String.valueOf(((MatchDTO) data).getId());
        } else if (data instanceof UserDTO) {
            return String.valueOf(((UserDTO) data).getId());
        } else if (data instanceof TicketDTO) {
            return String.valueOf(((TicketDTO) data).getId());
        }
        return null;
    }

    /**
     * Actually sends an update to all registered listeners
     */
    private void doActualBroadcast(Response update) {
        int listenerCount = listeners.size();
        logger.info("BROADCASTING UPDATE type={} to {} listeners", update.getType(), listenerCount);

        // Create a copy of the listeners list to avoid concurrent modification
        final List<BasketballJsonWorker> currentListeners = new ArrayList<>(listeners);

        // Track failed listeners for removal
        List<BasketballJsonWorker> failedListeners = Collections.synchronizedList(new ArrayList<>());

        // Use CountDownLatch to ensure all updates are processed before continuing
        CountDownLatch latch = new CountDownLatch(currentListeners.size());

        // Send to each listener using executor service for parallelism
        for (BasketballJsonWorker listener : currentListeners) {
            executorService.submit(() -> {
                try {
                    listener.sendUpdate(update);
                } catch (Exception e) {
                    logger.error("Error sending update to listener: {}", e.getMessage());
                    failedListeners.add(listener);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // Wait for all updates to be processed with a reasonable timeout
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            if (!completed) {
                logger.warn("Timed out waiting for update delivery");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for update delivery", e);
        }

        // Clean up failed listeners
        if (!failedListeners.isEmpty()) {
            listeners.removeAll(failedListeners);
            logger.warn("Removed {} failed listeners", failedListeners.size());
        }
    }


}