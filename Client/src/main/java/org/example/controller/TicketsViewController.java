package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Ticket;
import org.example.domain.User;
import org.example.service.ServicesException;
import org.example.service.interfaces.TicketServiceInterface;

import java.util.List;

public class TicketsViewController {
    @FXML private TableView<Ticket> ticketsTable;

    private TicketServiceInterface ticketService;
    private User currentUser;

    private static final Logger logger = LogManager.getLogger(TicketsViewController.class);

    @FXML
    public void initialize() {
        // This will be called after FXML loading
    }

    public void setTicketService(TicketServiceInterface ticketService) {
        this.ticketService = ticketService;
        if (currentUser != null) {
            loadTickets();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (ticketService != null) {
            loadTickets();
        }
    }

    private void loadTickets() {
        try {
            List<Ticket> tickets = ticketService.findTicketsBoughtByUser(currentUser);
            ticketsTable.setItems(FXCollections.observableArrayList(tickets));
        } catch (ServicesException e) {
            logger.error("Error loading tickets", e);
            showErrorAlert("Error Loading Tickets", e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) ticketsTable.getScene().getWindow()).close();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}