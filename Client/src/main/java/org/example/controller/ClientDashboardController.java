package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.User;
import org.example.network.rpc.BasketballServicesProxy;
import org.example.service.ServicesException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ClientDashboardController extends BaseController {
    @FXML private TableView<Match> matchesTable;

    private static final Logger logger = LogManager.getLogger(ClientDashboardController.class);

    @FXML
    private void initialize() {
        // Add row styling based on available tickets
        matchesTable.setRowFactory(tv -> {
            TableRow<Match> row = new TableRow<Match>() {
                @Override
                protected void updateItem(Match match, boolean empty) {
                    super.updateItem(match, empty);

                    if (empty || match == null) {
                        setStyle("");
                    } else if (match.getAvailableTickets() <= 0) {
                        setStyle("-fx-background-color: #ffeeee;"); // Light red for sold out
                    } else {
                        setStyle("");
                    }
                }
            };
            return row;
        });

        // Add window close handler
        Platform.runLater(() -> {
            if (matchesTable.getScene() != null && matchesTable.getScene().getWindow() != null) {
                initializeCloseHandler((Stage) matchesTable.getScene().getWindow());
            }
        });
    }

    @Override
    public void setServices(BasketballServicesProxy basketballServicesProxy) {
        super.setServices(basketballServicesProxy);
        loadMatches();
    }

    @Override
    protected boolean isRelevantMatch(Match match) {
        // All matches are relevant to the client dashboard
        return true;
    }

    @Override
    protected boolean isRelevantTicket(Ticket ticket) {
        // Only tickets for matches displayed in the table are relevant
        if (ticket == null || ticket.getMatch() == null) {
            return false;
        }

        Long matchId = ticket.getMatch().getId();
        return matchesTable.getItems().stream()
                .anyMatch(m -> m.getId().equals(matchId));
    }

    @Override
    protected void updateUIForMatch(Match match) {
        Platform.runLater(() -> {
            boolean matchFound = false;
            for (Match existingMatch : matchesTable.getItems()) {
                if (existingMatch.getId().equals(match.getId())) {
                    // Create a new match object instead of modifying in-place
                    int index = matchesTable.getItems().indexOf(existingMatch);
                    Match updatedMatch = new Match(
                            existingMatch.getId(),
                            existingMatch.getTeamA(),
                            existingMatch.getTeamB(),
                            existingMatch.getDateTime()
                    );
                    updatedMatch.setAvailableTickets(match.getAvailableTickets());
                    updatedMatch.setPriceRange(match.getPriceRange());

                    // Replace in the list to force property change events
                    matchesTable.getItems().set(index, updatedMatch);
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                loadMatches();
            } else {
                matchesTable.refresh();
            }
        });
    }

    @Override
    protected void updateUIForTicket(Ticket ticket) {
        // For ticket updates, we just need to update the related match
        try {
            Optional<Match> matchOpt = basketballServicesProxy.findOne(ticket.getMatch().getId());
            matchOpt.ifPresent(this::updateUIForMatch);
        } catch (Exception e) {
            logger.error("Error getting match data for ticket update", e);
        }
    }

    @Override
    protected void updateUIForUserTickets(User user) {
        // No direct UI update needed since this controller doesn't show user tickets
        // However, we could show a notification
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Tickets Updated");
            alert.setHeaderText(null);
            alert.setContentText("Your tickets have been updated. Check 'My Tickets' to see changes.");
            alert.show();
        });
    }

    private void loadMatches() {
        try {
            logger.debug("Loading matches for client dashboard");
            List<Match> matches = basketballServicesProxy.findAll();
            logger.debug("Loaded {} matches from service", matches.size());

            // For each match, get available tickets count and price range
            for (Match match : matches) {
                int availableTickets = basketballServicesProxy.countAvailableTicketsByMatch(match.getId());
                String priceRange = basketballServicesProxy.ticketPriceRangePerMatch(match.getId());
                match.setAvailableTickets(availableTickets);
                match.setPriceRange(priceRange);
                logger.debug("Match {} has {} available tickets", match.getMatchDescription(), availableTickets);
            }

            // Replace the entire table content
            Platform.runLater(() -> {
                matchesTable.getItems().clear();
                matchesTable.getItems().addAll(matches);
                matchesTable.refresh();
                logger.debug("Client dashboard refreshed with {} matches", matches.size());
            });
        } catch (ServicesException e) {
            logger.error("Error loading matches for client dashboard", e);
            showErrorAlert("Error Loading Matches", e.getMessage());
        }
    }

    @FXML
    private void handleViewTickets() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ticketsView.fxml"));
            Parent root = loader.load();

            TicketsViewController controller = loader.getController();

            // Set services and current user
            controller.setServices(basketballServicesProxy);
            controller.setCurrentUser(currentUser);

            // Force initial load of tickets
            controller.loadTickets();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("My Tickets");

            // Set up close handler
            controller.initializeCloseHandler(stage);

            stage.show();

            logger.info("Opened tickets view for user {}", currentUser.getUsername());
        } catch (IOException e) {
            logger.error("Error loading tickets view", e);
            showErrorAlert("Error Loading Tickets View", e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        cleanup();
        Stage stage = (Stage) matchesTable.getScene().getWindow();
        stage.close();
    }

    @Override
    protected void cleanup() {
        logger.info("Cleaning up client dashboard resources");
        super.cleanup();
    }
}