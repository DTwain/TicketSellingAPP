package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.User;
import org.example.service.AllServices;
import org.example.service.ServicesException;
import org.example.utils.events.ChangeEventType;
import org.example.utils.events.EntityChangeType;
import org.example.utils.observer.Observer;

import java.util.List;

public class TicketsViewController implements Observer<EntityChangeType<?>> {
    @FXML private TableView<Ticket> ticketsTable;

    private AllServices services;
    private User currentUser;

    @FXML
    public void initialize() {
        // This will be called after FXML loading
        if (currentUser != null && services != null) {
            loadTickets();
        }
    }

    public void setServices(AllServices services) {
        this.services = services;
        services.getTicketService().addObserver(this);
        if (currentUser != null) {
            loadTickets();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (services != null) {
            loadTickets();
        }
    }

    public void loadTickets() {
        try {
            List<Ticket> tickets = services.getTicketService().findTicketsBoughtByUser(currentUser);
            ticketsTable.getItems().setAll(tickets);
        } catch (ServicesException e) {
            showErrorAlert("Error Loading Tickets", e.getMessage());
            e.printStackTrace();
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

    @Override
    public void update(EntityChangeType<?> entityChangeType) {
        ChangeEventType type = entityChangeType.getType();
        Object newData = entityChangeType.getData();
        Object oldData = entityChangeType.getOldData();

        if(newData instanceof Ticket) {
            handleTicketEvent(type, (Ticket)newData, (Ticket)oldData);
        }
    }

    private void handleTicketEvent(ChangeEventType type, Ticket match1, Ticket match2) {
        switch (type) {
            case ADD:
                loadTickets();
                break;
            case UPDATE:
                loadTickets();
                break;
            case DELETE:
                loadTickets();
                break;
        }
    }
}