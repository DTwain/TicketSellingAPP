package org.example.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.network.rpc.BasketballServicesProxy;
import org.example.service.ServicesException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SellerDashboardController extends BaseController {
    @FXML private TableView<Match> matchesTable;
    @FXML private TableColumn<Match, String> actionsColumn;

    private static final Logger logger = LogManager.getLogger(SellerDashboardController.class);

    @FXML
    public void initialize() {
        // Configure the actions column to show a button or "SOLD OUT" text
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button sellButton = new Button("Sell Tickets");

            {
                sellButton.setOnAction(event -> {
                    Match match = getTableView().getItems().get(getIndex());
                    showSellTicketsDialog(match);
                });
                sellButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Match match = getTableView().getItems().get(getIndex());
                    if (match.getAvailableTickets() <= 0) {
                        setText("SOLD OUT");
                        setStyle("-fx-text-fill: red;");
                        setGraphic(null);
                    } else {
                        setText(null);
                        setGraphic(sellButton);
                    }
                }
            }
        });

        // Add row styling for better visual feedback
        matchesTable.setRowFactory(tv -> {
            TableRow<Match> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldMatch, newMatch) -> {
                if (newMatch != null) {
                    // Ensure row updates when available tickets change
                    if (newMatch.getAvailableTickets() <= 0) {
                        row.setStyle("-fx-background-color: #ffeeee;"); // Light red for sold out
                    } else {
                        row.setStyle("");
                    }
                }
            });
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
        // All matches are relevant to the seller dashboard
        return true;
    }

    @Override
    protected boolean isRelevantTicket(Ticket ticket) {
        // All tickets are relevant as they affect available seats
        return true;
    }

    @Override
    protected void updateUIForMatch(Match match) {
        boolean matchFound = false;
        boolean availabilityChanged = false;

        // First try to update the specific match in the table
        for (Match existingMatch : matchesTable.getItems()) {
            if (existingMatch.getId().equals(match.getId())) {
                matchFound = true;

                // Check if availability has changed
                if (existingMatch.getAvailableTickets() != match.getAvailableTickets()) {
                    availabilityChanged = true;
                }

                // Update match data
                existingMatch.setAvailableTickets(match.getAvailableTickets());
                existingMatch.setPriceRange(match.getPriceRange());

                logger.info("Updated match {} to {} available tickets in table",
                        match.getId(), match.getAvailableTickets());
                break;
            }
        }

        // Refresh the table UI
        matchesTable.refresh();

        // If match wasn't found OR availability changed (important UI update), do a full refresh
        if (!matchFound || availabilityChanged) {
            logger.info("Match {} not found or availability changed, performing full refresh", match.getId());
            loadMatches();
        }
    }

    @Override
    protected void updateUIForTicket(Ticket ticket) {
        // For ticket sales, update the related match
        try {
            if (ticket != null && ticket.getMatch() != null) {
                Optional<Match> matchOpt = basketballServicesProxy.findOne(ticket.getMatch().getId());
                matchOpt.ifPresent(this::updateUIForMatch);
            }
        } catch (Exception e) {
            logger.error("Error getting match data after ticket sale", e);
        }
    }

    @Override
    protected void updateUIForUserTickets(User user) {
        // Not directly relevant for sellers, no UI update needed
    }

    private void loadMatches() {
        try {
            logger.debug("Loading matches for seller dashboard");
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
                logger.debug("Seller dashboard refreshed with {} matches", matches.size());
            });
        } catch (ServicesException e) {
            logger.error("Error loading matches for seller dashboard", e);
            showErrorAlert("Error Loading Matches", e.getMessage());
        }
    }

    private void showSellTicketsDialog(Match match) {
        Dialog<TicketSale> dialog = new Dialog<>();
        dialog.setTitle("Sell Tickets");
        dialog.setHeaderText("Sell tickets for: " + match.getMatchDescription());

        // Set up form fields
        TextField nameField = new TextField();
        nameField.setPromptText("Customer Name");
        TextField addressField = new TextField();
        addressField.setPromptText("Customer Address");

        // Make sure we respect available tickets limit
        int maxTickets = Math.max(1, match.getAvailableTickets());
        Spinner<Integer> seatsSpinner = new Spinner<>(1, maxTickets, 1);

        // Show warning if match is close to sold out
        Label warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: #d9534f;");
        if (match.getAvailableTickets() < 5) {
            warningLabel.setText("Only " + match.getAvailableTickets() + " tickets available!");
        }

        VBox content = new VBox(10,
                new Label("Customer Name:"), nameField,
                new Label("Customer Address:"), addressField,
                new Label("Number of Seats:"), seatsSpinner,
                warningLabel);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        // Add buttons
        ButtonType sellButtonType = new ButtonType("Sell", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sellButtonType, ButtonType.CANCEL);

        // Convert result to TicketSale when Sell button is clicked
        dialog.setResultConverter(buttonType -> {
            if (buttonType == sellButtonType) {
                return new TicketSale(
                        match.getId(),
                        nameField.getText().trim(),
                        addressField.getText().trim(),
                        seatsSpinner.getValue()
                );
            }
            return null;
        });

        // Process the sale
        dialog.showAndWait().ifPresent(ticketSale -> {
            try {
                if (ticketSale.getCustomerName().isEmpty() || ticketSale.getCustomerAddress().isEmpty()) {
                    showErrorAlert("Invalid Input", "Customer name and address cannot be empty");
                    return;
                }

                // Display processing indicator
                ProgressIndicator progress = new ProgressIndicator();
                Stage progressStage = new Stage();
                progressStage.setScene(new Scene(new VBox(10, new Label("Processing sale..."), progress), 200, 100));
                progressStage.show();

                // Process sale in background
                new Thread(() -> {
                    try {
                        Double ticketsValue = basketballServicesProxy.sellTickets(ticketSale);

                        // Update UI on success - but network notification will handle actual UI update
                        Platform.runLater(() -> {
                            progressStage.close();
                            showSuccessAlert("Success", "Successfully sold " + ticketSale.getSeatsPurchased() +
                                    " ticket(s) to " + ticketSale.getCustomerName() +
                                    " for $" + String.format("%.2f", ticketsValue));
                        });
                    } catch (ServicesException e) {
                        Platform.runLater(() -> {
                            progressStage.close();
                            logger.error("Error selling tickets", e);
                            showErrorAlert("Sale Failed", e.getMessage());
                        });
                    }
                }).start();

            } catch (Exception e) {
                logger.error("Error processing ticket sale", e);
                showErrorAlert("Sale Failed", "An unexpected error occurred: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleLogout() {
        cleanup();
        Stage stage = (Stage) matchesTable.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleViewSales(ActionEvent actionEvent) {
        try {
            // Create a new stage for the search dialog
            Stage searchStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CustomerSearchDialog.fxml"));
            Parent root = loader.load();

            // Get the controller and set up the services
            CustomerSearchController controller = loader.getController();
            controller.setServices(basketballServicesProxy);

            // Set the current user so the search controller can register as observer
            controller.setCurrentUser(currentUser);

            // Set up the stage
            searchStage.setTitle("Search Customer Tickets");
            searchStage.setScene(new Scene(root));
            searchStage.show();
        } catch (IOException e) {
            logger.error("Error loading search dialog", e);
            showErrorAlert("Error Loading Search Dialog", e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        logger.info("Cleaning up seller dashboard resources");
        super.cleanup();
    }
}