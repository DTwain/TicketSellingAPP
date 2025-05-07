package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.controller.LoginController;
import org.example.domain.UserType;
import org.example.network.rpc.BasketballServicesProxy;
import org.example.service.AllServices;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class MainFX extends Application {
    private static final Logger logger = LogManager.getLogger(MainFX.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load client properties
            Properties clientProps = new Properties();
            try {
                clientProps.load(MainFX.class.getResourceAsStream("/client.properties"));
                logger.info("Client properties loaded");
            } catch (IOException e) {
                logger.error("Cannot find client.properties", e);
                showErrorAlert("Configuration Error", "Cannot find client.properties file.");
                return;
            }

            // Get server connection details
            String serverIP = clientProps.getProperty("server.host", "localhost");
            int serverPort;
            try {
                serverPort = Integer.parseInt(clientProps.getProperty("server.port", "55556"));
            } catch (NumberFormatException e) {
                logger.error("Invalid port number", e);
                showErrorAlert("Configuration Error", "Invalid server port number.");
                return;
            }

            // Create the services proxy
            BasketballServicesProxy servicesProxy = new BasketballServicesProxy(serverIP, serverPort);

            // Load the login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/loginAndSignUp.fxml"));
            Parent root = loader.load();

            // Set up the controller
            LoginController loginController = loader.getController();
            loginController.setServices(servicesProxy);

            // Load other views in advance
            FXMLLoader clientLoader = new FXMLLoader(getClass().getResource("/views/clientDashboard.fxml"));
            Parent clientRoot = clientLoader.load();
            loginController.setClientView(clientRoot, clientLoader.getController());

            FXMLLoader sellerLoader = new FXMLLoader(getClass().getResource("/views/sellerDashboard.fxml"));
            Parent sellerRoot = sellerLoader.load();
            loginController.setSellerView(sellerRoot, sellerLoader.getController());

            // Set up and show the primary stage
            primaryStage.setTitle("Basketball Ticket Shop");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();

            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.error("Error starting application", e);
            showErrorAlert("Application Error", "Error starting application: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}