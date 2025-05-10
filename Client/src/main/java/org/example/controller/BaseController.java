package org.example.controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.User;
import org.example.network.rpc.BasketballServicesProxy;
import org.example.service.ServicesException;
import org.example.utils.observer.TicketObserver;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base controller that handles common UI update behaviors
 * All controllers should extend this class to benefit from network-based updates
 */
public abstract class BaseController implements TicketObserver {
    protected final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    protected BasketballServicesProxy basketballServicesProxy;
    protected User currentUser;

    protected static final Logger logger = LogManager.getLogger(BaseController.class);

    // Add below the existing fields (underneath "protected User currentUser;" line)
    private final Queue<Runnable> pendingUpdates = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean processingUpdates = new AtomicBoolean(false);

    /**
     * Initialize window close handler to clean up resources
     */
    protected void initializeCloseHandler(Stage stage) {
        if (stage != null) {
            // Use a more reliable method to track when the window is closed
            stage.setOnCloseRequest(event -> {
                logger.info("Window close requested - cleaning up resources");
                cleanup();
            });
        } else {
            logger.warn("Cannot initialize close handler - stage is null");
        }
    }

    /**
     * Set the services proxy and register as observer if user is set
     */
    public void setServices(BasketballServicesProxy basketballServicesProxy) {
        this.basketballServicesProxy = basketballServicesProxy;

        // Register as observer if user is already set
        if (currentUser != null) {
            registerAsObserver();
        }
    }

    /**
     * Set the current user and register as observer if services are set
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // Register as observer if services are already set
        if (basketballServicesProxy != null) {
            registerAsObserver();
        }
    }

    /**
     * Register as observer with the service
     */
    protected void registerAsObserver() {
        try {
            if (basketballServicesProxy != null && currentUser != null) {
                // Check if we've already registered to avoid duplicate registrations
                logger.info("Registering observer for user: {}", currentUser.getId());
                basketballServicesProxy.registerTicketObserver(currentUser, this);
                logger.info("Successfully registered as observer for user {}", currentUser.getId());
            } else {
                logger.warn("Cannot register as observer - service or user not set");
            }
        } catch (ServicesException e) {
            logger.error("Failed to register as observer", e);
            showErrorAlert("Observer Registration Failed", e.getMessage());
        }
    }

    /**
     * Unregister from service
     */
    protected void unregisterAsObserver() {
        try {
            if (basketballServicesProxy != null && currentUser != null) {
                logger.info("Unregistering observer for user: {}", currentUser.getId());
                basketballServicesProxy.unregisterTicketObserver(currentUser);
            }
        } catch (ServicesException e) {
            logger.error("Error unregistering observer", e);
        }
    }

    /**
     * Handle match updates
     */
    @Override
    public void matchUpdated(Match match) throws ServicesException {
        boolean relevant = isRelevantMatch(match);
        logger.debug("matchUpdated: match={}, relevant={}", match.getId(), relevant);
        if (relevant) {
            safelyUpdateUI(() -> updateUIForMatch(match));
        }
    }

    public abstract void initialize();

    /**
     * Handle ticket sold updates
     */
    @Override
    public void ticketSold(Ticket ticket) throws ServicesException {
        boolean relevant = isRelevantTicket(ticket);
        logger.debug("ticketSold: ticket={}, match={}, relevant={}",
                ticket.getId(), ticket.getMatch().getId(), relevant);
        if (relevant) {
            safelyUpdateUI(() -> updateUIForTicket(ticket));
        }
    }

    /**
     * Handle user tickets changed updates
     */
    @Override
    public void userTicketsChanged(User user) throws ServicesException {
        boolean relevant = isRelevantUser(user);
        logger.debug("userTicketsChanged: user={}, currentUser={}, relevant={}",
                user.getId(),
                currentUser != null ? currentUser.getId() : "null",
                relevant);

        if (relevant) {
            safelyUpdateUI(() -> updateUIForUserTickets(user));
        }
    }

    /**
     * Safely update UI with debouncing
     */
    protected void safelyUpdateUI(Runnable updateAction) {
        // Queue the update
        pendingUpdates.offer(updateAction);

        // Start processing if not already in progress
        if (processingUpdates.compareAndSet(false, true)) {
            Platform.runLater(this::processNextUpdate);
        }
    }

    private void processNextUpdate() {
        try {
            Runnable nextUpdate = pendingUpdates.poll();
            if (nextUpdate != null) {
                try {
                    nextUpdate.run();
                } catch (Exception e) {
                    logger.error("Error in UI update", e);
                }

                // Schedule next update if queue is not empty
                if (!pendingUpdates.isEmpty()) {
                    Platform.runLater(this::processNextUpdate);
                    return;
                }
            }
        } finally {
            processingUpdates.set(false);

            // Check if new updates were added while processing
            if (!pendingUpdates.isEmpty() && processingUpdates.compareAndSet(false, true)) {
                Platform.runLater(this::processNextUpdate);
            }
        }
    }

    /**
     * Determine if a match update is relevant to this controller
     */
    protected boolean isRelevantMatch(Match match) {
        return true; // Override in subclasses
    }

    /**
     * Determine if a ticket update is relevant to this controller
     */
    protected boolean isRelevantTicket(Ticket ticket) {
        if (ticket == null) {
            return false;
        }

        // Check if this ticket belongs to current user
        if (ticket.getUser().isPresent() && currentUser != null &&
                ticket.getUser().get().getId().equals(currentUser.getId())) {
            return true;
        }

        // By default, accept all tickets for controllers that don't override
        return true;
    }

    /**
     * Determine if a user tickets update is relevant to this controller
     */
    protected boolean isRelevantUser(User user) {
        return currentUser != null && user != null &&
                currentUser.getId().equals(user.getId());
    }

    /**
     * Update UI for a match change - implement in subclasses
     */
    protected abstract void updateUIForMatch(Match match);

    /**
     * Update UI for a ticket sale - implement in subclasses
     */
    protected abstract void updateUIForTicket(Ticket ticket);

    /**
     * Update UI for user tickets change - implement in subclasses
     */
    protected abstract void updateUIForUserTickets(User user);

    /**
     * Show error alert
     */
    protected void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show success alert
     */
    protected void showSuccessAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Clean up resources
     */
    protected void cleanup() {
        unregisterAsObserver();
    }
}