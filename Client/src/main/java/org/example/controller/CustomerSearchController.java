package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Ticket;
import org.example.service.ServicesException;
import org.example.service.interfaces.TicketServiceInterface;

import java.util.List;

public class CustomerSearchController {
    @FXML private TextField nameField;
    @FXML private TableView<Ticket> resultsTable;

    private TicketServiceInterface ticketService;

    private static final Logger logger = LogManager.getLogger(CustomerSearchController.class);

    public void setTicketService(TicketServiceInterface ticketService) {
        this.ticketService = ticketService;
    }

    @FXML
    private void handleSearch() {
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            showErrorAlert("Search Error", "Please enter a customer name");
            return;
        }

        try {
            List<Ticket> results = ticketService.findTicketsByCustomer(name);
            resultsTable.setItems(FXCollections.observableArrayList(results));

            if (results.isEmpty()) {
                showInfoAlert("No Results", "No tickets found for customer: " + name);
            }
        } catch (ServicesException e) {
            logger.error("Error searching for tickets", e);
            showErrorAlert("Search Failed", e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) resultsTable.getScene().getWindow()).close();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}