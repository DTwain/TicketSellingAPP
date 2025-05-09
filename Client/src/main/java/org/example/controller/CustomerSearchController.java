package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.User;
import org.example.service.ServicesException;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CustomerSearchController extends BaseController {
    @FXML private TextField nameField;
    @FXML private TableView<Ticket> resultsTable;

    // Flag to track if search results are currently displayed
    private boolean hasActiveSearch = false;
    private String currentSearchTerm = "";
    private final Set<Long> displayedTicketIds = ConcurrentHashMap.newKeySet();

    private static final Logger logger = LogManager.getLogger(CustomerSearchController.class);

    @FXML
    private void initialize() {
        logger.debug("Initializing CustomerSearchController");

        // Add listener to track when search is active
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            hasActiveSearch = !newValue.trim().isEmpty();
            if (hasActiveSearch) {
                currentSearchTerm = newValue.trim();
            } else {
                currentSearchTerm = "";
            }
        });

        // Ensure we're registered as an observer when loaded
        Platform.runLater(() -> {
            if (basketballServicesProxy != null && currentUser != null) {
                try {
                    // Re-register to ensure we're in the observer list
                    basketballServicesProxy.registerTicketObserver(currentUser, this);
                    logger.debug("Explicitly registered CustomerSearchController as observer for user {}",
                            currentUser.getId());
                } catch (Exception e) {
                    logger.error("Failed to register observer in initialize: {}", e.getMessage());
                }
            }

            // Window close handler setup
            if (resultsTable.getScene() != null && resultsTable.getScene().getWindow() != null) {
                initializeCloseHandler((Stage) resultsTable.getScene().getWindow());
            }
        });
    }


    @Override
    public void ticketSold(Ticket ticket) throws ServicesException {
        logger.debug("ticketSold in CustomerSearchController: ticket={}, matchId={}, hasActiveSearch={}, searchTerm={}",
                ticket.getId(), ticket.getMatch().getId(), hasActiveSearch, currentSearchTerm);

        // Check if this ticket belongs to user we're currently viewing
        if (hasActiveSearch &&
                ticket.getUser().isPresent() &&
                ticket.getUser().get().getUsername().equalsIgnoreCase(currentSearchTerm)) {

            logger.info("New ticket sold to currently viewed customer - refreshing search results");
            safelyUpdateUI(this::forceRefreshSearch);
        } else {
            super.ticketSold(ticket);
        }
    }

    private void forceRefreshSearch() {
        if (!hasActiveSearch || currentSearchTerm.isEmpty()) {
            logger.debug("Cannot force refresh search - no active search");
            return;
        }

        try {
            logger.info("Force refreshing search for customer: {}", currentSearchTerm);
            List<Ticket> results = basketballServicesProxy.findTicketsByCustomer(currentSearchTerm);
            logger.info("Force refresh found {} tickets for customer {}", results.size(), currentSearchTerm);

            // Update tracking
            displayedTicketIds.clear();
            displayedTicketIds.addAll(results.stream()
                    .map(Ticket::getId)
                    .collect(Collectors.toSet()));

            // Update UI
            Platform.runLater(() -> {
                resultsTable.getItems().clear();
                resultsTable.getItems().addAll(results);
                resultsTable.refresh();
                logger.debug("Search results refreshed with {} tickets", results.size());
            });
        } catch (Exception e) {
            logger.error("Error force-refreshing search: {}", e.getMessage(), e);
        }
    }


    @Override
    protected boolean isRelevantMatch(Match match) {
        // A match is relevant if it appears in any displayed ticket
        return match != null && hasActiveSearch &&
                resultsTable.getItems().stream()
                        .anyMatch(t -> t.getMatch() != null &&
                                t.getMatch().getId().equals(match.getId()));
    }

    @Override
    protected boolean isRelevantTicket(Ticket ticket) {
        // Make this more permissive - accept tickets even if no search is active
        if (ticket == null || ticket.getUser().isEmpty()) {
            return false;
        }

        String ticketCustomerName = ticket.getUser().get().getUsername();

        // Either it's in the current display...
        boolean isInCurrentResults = displayedTicketIds.contains(ticket.getId());

        // ...or it matches the search term (if search is active)
        boolean matchesSearch = hasActiveSearch &&
                !currentSearchTerm.isEmpty() &&
                ticketCustomerName != null &&
                ticketCustomerName.equalsIgnoreCase(currentSearchTerm);

        return isInCurrentResults || matchesSearch;
    }

    @Override
    protected boolean isRelevantUser(User user) {
        // For a search controller, a user update is relevant if that user matches our current search
        return hasActiveSearch &&
                user != null &&
                user.getUsername() != null &&
                currentSearchTerm != null &&
                user.getUsername().equalsIgnoreCase(currentSearchTerm);
    }

    @Override
    protected void updateUIForMatch(Match match) {
        // Update match information in displayed tickets
        boolean needsRefresh = false;

        for (Ticket ticket : resultsTable.getItems()) {
            if (ticket.getMatch() != null && ticket.getMatch().getId().equals(match.getId())) {
                ticket.setMatch(match);
                needsRefresh = true;
            }
        }

        if (needsRefresh) {
            resultsTable.refresh();
        }
    }

    @Override
    protected void updateUIForTicket(Ticket ticket) {
        // If this is a ticket already in our results, update it
        if (displayedTicketIds.contains(ticket.getId())) {
            for (int i = 0; i < resultsTable.getItems().size(); i++) {
                Ticket existingTicket = resultsTable.getItems().get(i);
                if (existingTicket.getId().equals(ticket.getId())) {
                    resultsTable.getItems().set(i, ticket);
                    break;
                }
            }
            resultsTable.refresh();
        }
        // If this might be a new ticket for our current search, refresh the search
        else if (hasActiveSearch && ticket.getUser().isPresent() &&
                ticket.getUser().get().getUsername().equalsIgnoreCase(currentSearchTerm)) {
            // Use safelyUpdateUI to avoid threading issues
            safelyUpdateUI(this::handleSearch);
        }
    }

    @Override
    protected void updateUIForUserTickets(User user) {
        // If we're searching for this user, refresh results
        if (hasActiveSearch &&
                user != null &&
                user.getUsername() != null &&
                user.getUsername().equalsIgnoreCase(currentSearchTerm)) {
            // Use safelyUpdateUI to queue the update
            safelyUpdateUI(this::handleSearch);
        }
    }

    @FXML
    public void handleSearch() {
        String name = nameField.getText().trim();
        hasActiveSearch = !name.isEmpty();
        currentSearchTerm = name;

        if (!hasActiveSearch) {
            showErrorAlert("Search Error", "Please enter a customer name");
            return;
        }

        try {
            logger.info("Searching for tickets purchased by customer: {}", name);
            List<Ticket> results = basketballServicesProxy.findTicketsByCustomer(name);

            logger.info("Found {} tickets for customer {}", results.size(), name);

            // Update set of displayed ticket IDs
            displayedTicketIds.clear();
            displayedTicketIds.addAll(
                    results.stream()
                            .map(Ticket::getId)
                            .collect(Collectors.toSet())
            );

            // Update UI with results
            Platform.runLater(() -> {
                resultsTable.getItems().clear();
                resultsTable.getItems().addAll(results);
                resultsTable.refresh();
            });

        } catch (ServicesException e) {
            logger.error("Search failed: {}", e.getMessage(), e);
            showErrorAlert("Search Failed", e.getMessage());
        }
    }

    @Override
    public void userTicketsChanged(User user) throws ServicesException {
        logger.debug("userTicketsChanged in CustomerSearchController: userId={}, searchTerm={}, hasActiveSearch={}",
                user.getId(), currentSearchTerm, hasActiveSearch);

        // Simplify condition - if we're actively searching and any user's tickets change, refresh
        if (hasActiveSearch && !currentSearchTerm.isEmpty()) {
            logger.info("Force refreshing search for term: {}", currentSearchTerm);
            safelyUpdateUI(this::forceRefreshSearch);
        } else {
            super.userTicketsChanged(user);
        }
    }

    @FXML
    private void handleClose() {
        cleanup();
        ((Stage) resultsTable.getScene().getWindow()).close();
    }

    @Override
    protected void cleanup() {
        logger.info("Cleaning up CustomerSearchController resources");
        hasActiveSearch = false;
        currentSearchTerm = "";
        displayedTicketIds.clear();
        super.cleanup();
    }
}