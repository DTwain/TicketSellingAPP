package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.TicketSale;
import org.example.service.ServicesException;
import org.example.service.interfaces.MatchServiceInterface;
import org.example.service.interfaces.TicketServiceInterface;
import org.example.service.interfaces.UserServiceInterface;

import java.io.IOException;
import java.util.List;

public class SellerDashboardController {
    @FXML private TableView<Match> matchesTable;
    @FXML private TableColumn<Match, String> actionsColumn;

    private MatchServiceInterface matchService;
    private TicketServiceInterface ticketService;
    private UserServiceInterface userService;
    private String currentUsername;

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
    }

    public void setServices(UserServiceInterface userService) {
        this.userService = userService;
        // In a real application, these services would be injected properly
    }

    public void setMatchService(MatchServiceInterface matchService) {
        this.matchService = matchService;
        loadMatches();
    }

    public void setTicketService(TicketServiceInterface ticketService) {
        this.ticketService = ticketService;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        logger.info("Current seller username set: {}", username);
    }

    private void loadMatches() {
        try {
            List<Match> matches = matchService.findAll();

            // For each match, get available tickets count and price range
            for (Match match : matches) {
                int availableTickets = ticketService.countAvailableTicketsByMatch(match.getId());
                String priceRange = ticketService.ticketPriceRangePerMatch(match.getId());
                match.setAvailableTickets(availableTickets);
                match.setPriceRange(priceRange);
            }

            matchesTable.setItems(FXCollections.observableArrayList(matches));
        } catch (ServicesException e) {
            logger.error("Error loading matches", e);
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
        Spinner<Integer> seatsSpinner = new Spinner<>(1, match.getAvailableTickets(), 1);

        VBox content = new VBox(10,
                new Label("Customer Name:"), nameField,
                new Label("Customer Address:"), addressField,
                new Label("Number of Seats:"), seatsSpinner);
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

                Double ticketsValue = ticketService.sellTickets(ticketSale);
                loadMatches(); // Refresh the matches table
                showSuccessAlert("Success", "Successfully sold " + ticketSale.getSeatsPurchased() +
                        " ticket(s) to " + ticketSale.getCustomerName() +
                        " for $" + String.format("%.2f", ticketsValue));
            } catch (ServicesException e) {
                logger.error("Error selling tickets", e);
                showErrorAlert("Sale Failed", e.getMessage());
            }
        });
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        // Close the current window
        Stage stage = (Stage) matchesTable.getScene().getWindow();
        stage.close();

        // Open the login window again
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/loginAndSignUp.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setServices(userService);

            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Basketball Ticket Shop");
            loginStage.show();
        } catch (IOException e) {
            logger.error("Error returning to login screen", e);
            showErrorAlert("Error", "Could not return to login screen");
        }
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
            controller.setTicketService(ticketService);

            // Set up the stage
            searchStage.setTitle("Search Customer Tickets");
            searchStage.setScene(new Scene(root));
            searchStage.show();
        } catch (IOException e) {
            logger.error("Error loading search dialog", e);
            showErrorAlert("Error Loading Search Dialog", e.getMessage());
        }
    }
}