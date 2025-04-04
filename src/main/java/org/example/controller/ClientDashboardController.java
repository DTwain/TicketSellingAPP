package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.example.domain.Match;
import org.example.domain.User;
import org.example.service.AllServices;
import org.example.service.ServicesException;
import org.example.utils.events.ChangeEventType;
import org.example.utils.events.EntityChangeType;
import org.example.utils.observer.Observer;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ClientDashboardController implements Observer<EntityChangeType<?>> {
    @FXML private TableView<Match> matchesTable;

    private AllServices services;
    private User currentUser;

    @FXML
    private void initialize() {

    }

    public void setServices(AllServices services) {
        this.services = services;
        services.getTicketService().addObserver(this);
        services.getMatchService().addObserver(this);
        loadMatches();
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

    public void setCurrentUser(String username) {
        try {
            Optional<User> userOptional = services.getUserService().getUserByUsername(username);
            userOptional.ifPresent(user -> currentUser = user);
        } catch (ServicesException e) {
            showErrorAlert("User was not stored", "Verify...");
        }
    }


    @FXML
    private void handleViewTickets() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ticketsView.fxml"));
            Parent root = loader.load();

            TicketsViewController controller = loader.getController();
            controller.setServices(services);  // Make sure services is properly initialized
            controller.setCurrentUser(currentUser);  // Make sure currentUser is properly set

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showErrorAlert("Error Loading Tickets View", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        // Return to login screen
        Stage stage = (Stage) matchesTable.getScene().getWindow();
        stage.close();
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
}