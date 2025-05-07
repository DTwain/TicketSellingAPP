package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.User;
import org.example.service.ServicesException;
import org.example.service.interfaces.MatchServiceInterface;
import org.example.service.interfaces.TicketServiceInterface;
import org.example.service.interfaces.UserServiceInterface;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ClientDashboardController {
    @FXML private TableView<Match> matchesTable;

    private MatchServiceInterface matchService;
    private TicketServiceInterface ticketService;
    private UserServiceInterface userService;
    private User currentUser;

    private static final Logger logger = LogManager.getLogger(ClientDashboardController.class);

    @FXML
    private void initialize() {
        // Initialize any components if needed
    }

    public void setServices(UserServiceInterface userService) {
        this.userService = userService;
        // In a real application, you would get the other services from a shared service provider
        // For now, we'll simulate this by having them injected separately
    }

    public void setMatchService(MatchServiceInterface matchService) {
        this.matchService = matchService;
        loadMatches();
    }

    public void setTicketService(TicketServiceInterface ticketService) {
        this.ticketService = ticketService;
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

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void handleViewTickets() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ticketsView.fxml"));
            Parent root = loader.load();

            TicketsViewController controller = loader.getController();
            controller.setTicketService(ticketService);
            controller.setCurrentUser(currentUser);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("My Tickets");
            stage.show();
        } catch (IOException e) {
            logger.error("Error loading tickets view", e);
            showErrorAlert("Error Loading Tickets View", e.getMessage());
        }
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
}