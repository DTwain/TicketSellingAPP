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
import org.example.network.grpc.BasketballGrpcServicesProxy;
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

        // Add window close handler
        Platform.runLater(() -> {
            if (ticketsTable.getScene() != null && ticketsTable.getScene().getWindow() != null) {
                initializeCloseHandler((Stage) ticketsTable.getScene().getWindow());
            }
        });
    }

    @Override
    public void setServices(BasketballGrpcServicesProxy basketballServicesProxy) {
        super.setServices(basketballServicesProxy);

        // Add this controller as a direct observer
        if (basketballServicesProxy != null) {
            basketballServicesProxy.addObserverListener(this);
            logger.info("TicketsViewController registered as direct observer");
        }
    }

    @Override
    public void setCurrentUser(User user) {
        super.setCurrentUser(user);

        // Load tickets after user is set
        if (basketballServicesProxy != null && user != null) {
            Platform.runLater(this::loadTickets);
        }
    }

    public void loadTickets() {
        if (basketballServicesProxy == null || currentUser == null) {
            logger.warn("Cannot load tickets via gRPC - service or user not set");
            return;
        }

        try {
            logger.info("Loading tickets via gRPC for user: {}", currentUser.getUsername());

            // Fetch tickets from gRPC service
            List<Ticket> tickets = basketballServicesProxy.findTicketsBoughtByUser(currentUser);
            logger.info("Found {} tickets via gRPC for user {}", tickets.size(), currentUser.getUsername());

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

                logger.debug("Tickets view refreshed with {} tickets via gRPC", tickets.size());
            });
        } catch (ServicesException e) {
            logger.error("gRPC error loading tickets: {}", e.getMessage(), e);
            showErrorAlert("Error Loading Tickets", "gRPC connection error: " + e.getMessage());
        }
    }

    @Override
    protected boolean isRelevantMatch(Match match) {
        return match != null && currentUser != null;
    }

    @Override
    protected boolean isRelevantTicket(Ticket ticket) {
        return ticket != null &&
                ticket.getUser().isPresent() &&
                currentUser != null &&
                ticket.getUser().get().getId().equals(currentUser.getId());
    }

    @Override
    protected boolean isRelevantUser(User user) {
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
        } else if (isRelevantTicket(ticket)) {
            // This is a new ticket for current user, reload all tickets
            loadTickets();
        }
    }

    @Override
    protected void updateUIForUserTickets(User user) {
        logger.info("Updating tickets UI for user change: {}", user.getUsername());
        if (isRelevantUser(user)) {
            forceReloadTickets();
        }
    }

    @Override
    public void userTicketsChanged(User user) throws ServicesException {
        logger.info("TicketsViewController.userTicketsChanged: userId={}, currentUser={}",
                user.getId(), currentUser != null ? currentUser.getId() : "null");

        // Force a reload if this update is for our user
        if (isRelevantUser(user)) {
            logger.info("FORCING IMMEDIATE RELOAD of tickets for user {}", currentUser.getId());

            Platform.runLater(() -> {
                logger.info("Executing ticket reload on JavaFX thread for user {}", currentUser.getId());
                forceReloadTickets();
            });
        } else {
            logger.debug("UserTicketsChanged not relevant: update for user {}, current user {}",
                    user.getId(), currentUser != null ? currentUser.getId() : "null");
            super.userTicketsChanged(user);
        }
    }

    @Override
    public void ticketSold(Ticket ticket) throws ServicesException {
        logger.debug("TicketsViewController.ticketSold: ticketId={}, userId={}, currentUserId={}",
                ticket.getId(),
                ticket.getUser().isPresent() ? ticket.getUser().get().getId() : "null",
                currentUser != null ? currentUser.getId() : "null");

        if (isRelevantTicket(ticket)) {
            logger.info("Ticket sold for current user - forcing immediate reload");
            Platform.runLater(this::forceReloadTickets);
        } else {
            super.ticketSold(ticket);
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

                // Update ID tracking
                displayedTicketIds.clear();
                displayedTicketIds.addAll(tickets.stream()
                        .map(Ticket::getId)
                        .collect(Collectors.toSet()));

                // Update table - force a complete refresh
                ticketsTable.getItems().clear();
                ticketsTable.getItems().addAll(tickets);
                ticketsTable.refresh();
                logger.info("Tickets view force refreshed with {} tickets", tickets.size());

                // Show user feedback
                showSuccessAlert("Tickets Updated",
                        "Your tickets have been updated! You now have " + tickets.size() + " tickets.");
            }
        } catch (Exception e) {
            logger.error("Error force-reloading tickets: {}", e.getMessage(), e);
            showErrorAlert("Error Loading Tickets", "Failed to reload tickets: " + e.getMessage());
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