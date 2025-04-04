package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.domain.Ticket;
import org.example.service.AllServices;
import org.example.service.ServicesException;
import org.example.utils.events.EntityChangeType;
import org.example.utils.observer.Observer;

import java.util.List;

public class CustomerSearchController {
    @FXML private TextField nameField;
    @FXML private TableView<Ticket> resultsTable;

    private AllServices services;

    public void setServices(AllServices services) {
        this.services = services;
    }

    @FXML
    private void handleSearch() {
        String name = nameField.getText().trim();

        if (name.isEmpty() ) {
            showErrorAlert("Search Error", "Please enter at least name or address");
            return;
        }

        try {
            List<Ticket> results = services.getTicketService()
                    .findTicketsByCustomer(name);

            resultsTable.getItems().setAll(results);
        } catch (ServicesException e) {
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

}