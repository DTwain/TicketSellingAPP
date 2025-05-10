package org.example.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TicketsViewController extends BaseController {
    @FXML private TableView<Ticket> ticketsTable;

    private final Set<Long> displayedTicketIds = ConcurrentHashMap.newKeySet();

    private static final Logger logger = LogManager.getLogger(TicketsViewController.class);

    @FXML
    public void initialize() {
        logger.debug("Initializing TicketsViewController");


        logger.debug("Initializing TicketsViewController");

        // Add a direct observer for ticket updates
        if (BasketballServicesProxy.getInstance() != null) {
            BasketballServicesProxy.getInstance().addObserverListener(new TicketObserver() {
                @Override
                public void ticketSold(Ticket ticket) throws ServicesException {
                    // Refresh if the ticket is for the current user
                    if (currentUser != null && ticket.getUser().isPresent() &&
                            ticket.getUser().get().getId().equals(currentUser.getId())) {
                        logger.info("Direct observer: New ticket sold to current user, refreshing");
                        Platform.runLater(TicketsViewController.this::loadTickets);
                    }
                }

                @Override
                public void matchUpdated(Match match) throws ServicesException {
                    // No direct action needed for match updates in tickets view
                }

                @Override
                public void userTicketsChanged(User user) throws ServicesException {
                    // Refresh if this update is for the current user
                    if (currentUser != null && user.getId().equals(currentUser.getId())) {
                        logger.info("Direct observer: User tickets changed for current user, refreshing");
                        Platform.runLater(TicketsViewController.this::loadTickets);
                    }
                }
            });
        }

        // Ensure we get registered as an observer
        Platform.runLater(() -> {
            if (basketballServicesProxy != null && currentUser != null) {
                try {
                    // Re-register to ensure we're in the observer list
                    basketballServicesProxy.registerTicketObserver(currentUser, this);
                    logger.debug("Explicitly registered TicketsViewController as observer for user {}",
                            currentUser.getId());
                } catch (Exception e) {
                    logger.error("Failed to register observer in initialize: {}", e.getMessage());
                }
            }

            // Window close handler
            if (ticketsTable.getScene() != null && ticketsTable.getScene().getWindow() != null) {
                initializeCloseHandler((Stage) ticketsTable.getScene().getWindow());
            }
        });
    }

    @Override
    public void setCurrentUser(User user) {
        super.setCurrentUser(user);

        // Explicitly load tickets after user is set
        if (basketballServicesProxy != null && user != null) {
            Platform.runLater(this::loadTickets);
        }
    }

    public void loadTickets() {
        if (basketballServicesProxy == null || currentUser == null) {
            logger.warn("Cannot load tickets - service or user not set");
            return;
        }

        try {
            logger.info("Loading tickets for user: {}", currentUser.getUsername());

            // Fetch tickets from service
            List<Ticket> tickets = basketballServicesProxy.findTicketsBoughtByUser(currentUser);
            logger.info("Found {} tickets for user {}", tickets.size(), currentUser.getUsername());

            // Update set of displayed ticket IDs
            displayedTicketIds.clear();
            displayedTicketIds.addAll(
                    tickets.stream()
                            .map(Ticket::getId)
                            .collect(Collectors.toSet())
            );

            // Update UI safely
            Platform.runLater(() -> {
                ticketsTable.getItems().clear();
                ticketsTable.getItems().addAll(tickets);
                ticketsTable.refresh();

                logger.debug("Tickets view refreshed with {} tickets", tickets.size());
            });
        } catch (ServicesException e) {
            logger.error("Error loading tickets: {}", e.getMessage(), e);
            showErrorAlert("Error Loading Tickets", e.getMessage());
        }
    }

    @Override
    protected boolean isRelevantMatch(Match match) {
        // Simplify this to accept all matches for the current user
        // This will capture new tickets for matches we don't already display
        return match != null && currentUser != null;
    }

    @Override
    protected boolean isRelevantTicket(Ticket ticket) {
        // Accept any ticket for the current user
        return ticket != null &&
                ticket.getUser().isPresent() &&
                currentUser != null &&
                ticket.getUser().get().getId().equals(currentUser.getId());
    }

    @Override
    protected boolean isRelevantUser(User user) {
        // Always consider updates for the current user as relevant
        boolean relevant = currentUser != null && user != null &&
                currentUser.getId().equals(user.getId());

        logger.debug("isRelevantUser check: user={}, currentUser={}, relevant={}",
                user != null ? user.getId() : "null",
                currentUser != null ? currentUser.getId() : "null",
                relevant);

        return relevant;
    }

    @Override
    protected void updateUIForMatch(Match match) {
        // Update match details in tickets that reference this match
        boolean needsRefresh = false;

        for (Ticket ticket : ticketsTable.getItems()) {
            if (ticket.getMatch() != null && ticket.getMatch().getId().equals(match.getId())) {
                ticket.setMatch(match);
                needsRefresh = true;
            }
        }

        if (needsRefresh) {
            ticketsTable.refresh();
        }
    }

    @Override
    protected void updateUIForTicket(Ticket ticket) {
        // Check if this is a ticket we're already displaying
        if (displayedTicketIds.contains(ticket.getId())) {
            // Update the existing ticket
            for (int i = 0; i < ticketsTable.getItems().size(); i++) {
                Ticket existingTicket = ticketsTable.getItems().get(i);
                if (existingTicket.getId().equals(ticket.getId())) {
                    // Update ticket properties
                    ticketsTable.getItems().set(i, ticket);
                    break;
                }
            }
            ticketsTable.refresh();
        } else {
            // This is a new ticket, reload all tickets
            loadTickets();
        }
    }

    @Override
    protected void updateUIForUserTickets(User user) {
        logger.info("Updating tickets UI for user change: {}", user.getUsername());
        forceReloadTickets(); // Use the more robust method
    }

    private Ticket findTicketById(Long ticketId) {
        for (Ticket ticket : ticketsTable.getItems()) {
            if (ticket.getId().equals(ticketId)) {
                return ticket;
            }
        }
        return null;
    }


    @Override
    public void userTicketsChanged(User user) throws ServicesException {
        logger.debug("userTicketsChanged in TicketsViewController: userId={}, currentUser={}",
                user.getId(), currentUser != null ? currentUser.getId() : "null");

        // CHANGE: Use forceReloadTickets instead of loadTickets
        if (currentUser != null && user != null &&
                user.getId().equals(currentUser.getId())) {
            logger.info("Force reloading tickets for current user: {}", currentUser.getUsername());
            safelyUpdateUI(this::forceReloadTickets);  // Call the more robust method
        } else {
            super.userTicketsChanged(user);
        }
    }

    private void forceReloadTickets() {
        try {
            logger.info("Force reloading tickets for user: {}",
                    currentUser != null ? currentUser.getUsername() : "null");

            if (basketballServicesProxy != null && currentUser != null) {
                List<Ticket> tickets = basketballServicesProxy.findTicketsBoughtByUser(currentUser);
                logger.info("Force reload: Found {} tickets for user {}",
                        tickets.size(), currentUser.getUsername());

                // Important: Update on JavaFX thread!
                Platform.runLater(() -> {
                    // Update ID tracking
                    displayedTicketIds.clear();
                    displayedTicketIds.addAll(tickets.stream()
                            .map(Ticket::getId)
                            .collect(Collectors.toSet()));

                    // Update table - force a complete refresh
                    ticketsTable.getItems().clear();
                    ticketsTable.getItems().addAll(tickets);
                    ticketsTable.refresh();
                    logger.debug("Tickets view force refreshed with {} tickets", tickets.size());
                });
            }
        } catch (Exception e) {
            logger.error("Error force-reloading tickets: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleClose() {
        cleanup();

        // Close the window
        Stage stage = (Stage) ticketsTable.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    @Override
    protected void cleanup() {
        logger.info("Cleaning up TicketsViewController resources");
        displayedTicketIds.clear();
        super.cleanup();
    }
}