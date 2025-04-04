package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.example.domain.Match;
import org.example.domain.TicketSale;
import org.example.service.AllServices;
import org.example.service.ServicesException;
import org.example.utils.events.ChangeEventType;
import org.example.utils.events.EntityChangeType;
import org.example.utils.observer.Observer;

import java.io.IOException;
import java.util.List;

public class SellerDashboardController implements Observer<EntityChangeType<?>> {
    @FXML private TableView<Match> matchesTable;
    @FXML private TableColumn<Match, String> actionsColumn;

    private AllServices services;
    private String currentUsername;

    public void setServices(AllServices services) {
        this.services = services;
        services.getMatchService().addObserver(this);
        services.getTicketService().addObserver(this);
        loadMatches();
    }

    @FXML
    public void initialize() {
        // Initialize column bindings
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button btn = new Button("Sell Tickets");
            {
                btn.setOnAction(event -> {
                    Match match = getTableView().getItems().get(getIndex());
                    showSellTicketsDialog(match);
                });
                btn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                // Clear all previous content
                setText(null);
                setGraphic(null);
                setTextFill(Color.BLACK); // Reset text color

                if (!empty) {
                    Match match = getTableView().getItems().get(getIndex());
                    if (match.getAvailableTickets() <= 0) {
                        setText("SOLD OUT");
                        setTextFill(Color.RED);
                    } else {
                        setGraphic(btn);
                    }
                }
            }
        });
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

                Double ticketsValue = services.getTicketService().sellTickets(ticketSale);
                loadMatches(); // Refresh the matches table
                showSuccessAlert("Success", ticketsSoldMessage(ticketSale));
            } catch (ServicesException e) {
                showErrorAlert("Sale Failed", e.getMessage());
            }
        });
    }

    private String ticketsSoldMessage(TicketSale ticketSale) {
        return String.format("Successfully sold %d ticket(s) to %s",
                ticketSale.getSeatsPurchased(),
                ticketSale.getCustomerName());
    }
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    private void loadMatches() {
        try {
            List<Match> matches = (List<Match>) services.getMatchService().findAll();

            // For each match, get available tickets count
            for (Match match : matches) {
                int availableTickets = services.getTicketService()
                        .countAvailableTicketsByMatch(match.getId());
                String priceRange = services.getTicketService().ticketPriceRangePerMatch(match.getId());
                match.setAvailableTickets(availableTickets);
                match.setPriceRange(priceRange);
            }

            matchesTable.getItems().setAll(matches);
        } catch (ServicesException e) {
            showErrorAlert("Error Loading Matches", e.getMessage());
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    @FXML
    private void handleLogout() {
        Stage stage = (Stage) matchesTable.getScene().getWindow();
        stage.close();
    }


    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void update(EntityChangeType<?> entityChangeType) {
        ChangeEventType type = entityChangeType.getType();
        Object newData = entityChangeType.getData();
        Object oldData = entityChangeType.getOldData();

        if(newData instanceof Match) {
            handleMatchEvent(type, (Match)newData, (Match)oldData);
        }
    }

    private void handleMatchEvent(ChangeEventType type, Match match1, Match match2) {
        switch (type) {
            case ADD:
                loadMatches();
                break;
            case UPDATE:
                loadMatches();
                break;
            case DELETE:
                loadMatches();
                break;
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
            controller.setServices(services);

            // Set up the stage
            searchStage.setTitle("Search Customer Tickets");
            searchStage.setScene(new Scene(root));
            searchStage.show();
        } catch (IOException e) {
            showErrorAlert("Error Loading Search Dialog", e.getMessage());
        }
    }
}