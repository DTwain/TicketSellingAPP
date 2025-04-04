package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.service.AllServices;
import org.example.service.ServicesException;
import org.example.service.UserService;

import java.net.URL;

import java.io.IOException;

public class LoginController {
    @FXML private VBox loginForm;
    @FXML private VBox signupForm;

    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;

    @FXML private TextField signupUsername;
    @FXML private PasswordField signupPassword;
    @FXML private PasswordField signupConfirmPassword;

    @FXML private Button showSignupButton;
    @FXML private Button showLoginButton;

    @FXML private Button loginButton;
    @FXML private Button signupButton;

    private AllServices services;

    @FXML
    private void initialize() {
        // Set up button actions
        showSignupButton.setOnAction(e -> toggleForms());
        showLoginButton.setOnAction(e -> toggleForms());

        loginButton.setOnAction(e -> handleLogin());
        signupButton.setOnAction(e -> handleSignup());
    }

    public void setService(AllServices allServices) {
        this.services = allServices;
    }

    private void toggleForms() {
        boolean loginVisible = loginForm.isVisible();
        loginForm.setVisible(!loginVisible);
        loginForm.setManaged(!loginVisible);
        signupForm.setVisible(loginVisible);
        signupForm.setManaged(loginVisible);

        // Clear fields when switching forms
        if (loginVisible) {
            clearSignupFields();
        } else {
            clearLoginFields();
        }
    }

    @FXML
    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Failed", "Empty Fields",
                    "Please enter both username and password");
            return;
        }

        try {
            // First authenticate as regular user
            boolean userAuth = services.getUserService().authenticate(username, password);

            if (userAuth) {
                // Check if this user is a ticket seller
                if (services.getTicketSellerService().isTicketSeller(username)) {
                    showAlert(Alert.AlertType.INFORMATION, "Login Success", "Welcome Seller",
                            "You have successfully logged in as ticket seller!");
                    openSellerInterface();
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Login Success", "Welcome",
                            "You have successfully logged in!");
                    openClientInterface();
                }
                return;
            }

            // If authentication failed
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Credentials",
                    "The username or password is incorrect");
        } catch (ServicesException e) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Service Error",
                    "Failed to verify credentials: " + e.getMessage());
        }
    }

    @FXML
    private void handleSignup() {
        String username = signupUsername.getText().trim();
        String password = signupPassword.getText().trim();
        String confirmPassword = signupConfirmPassword.getText().trim();

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Signup Failed", "Empty Fields",
                    "Please fill in all required fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Signup Failed", "Password Mismatch",
                    "Password and confirmation password do not match");
            return;
        }

        try {
            // Check if username already exists
            if (services.getUserService().usernameExists(username)) {
                showAlert(Alert.AlertType.ERROR, "Signup Failed", "Username Taken",
                        "This username is already in use. Please choose another one.");
                return;
            }

            // Create new user
            boolean signupSuccess = services.getUserService().registerUser(username, password);
            if (signupSuccess) {
                showAlert(Alert.AlertType.INFORMATION, "Signup Success", "Account Created",
                        "Your account has been successfully created!");
                toggleForms(); // Switch back to login form
                clearSignupFields();
            } else {
                showAlert(Alert.AlertType.ERROR, "Signup Failed", "Registration Error",
                        "Failed to create account. Please try again.");
            }
        } catch (ServicesException e) {
            showAlert(Alert.AlertType.ERROR, "Signup Error", "Service Error",
                    "Failed to register user: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearLoginFields() {
        loginUsername.clear();
        loginPassword.clear();
    }

    private void clearSignupFields() {
        signupUsername.clear();
        signupPassword.clear();
        signupConfirmPassword.clear();
    }

    private void openClientInterface() {
        try {
            Stage clientStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/clientDashboard.fxml"));
            Parent root = loader.load();

            ClientDashboardController controller = loader.getController();
            controller.setServices(services);
            controller.setCurrentUser(loginUsername.getText().trim());


            clientStage.setScene(new Scene(root));
            clientStage.setTitle("Client Dashboard");
            clientStage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open client interface",
                    e.getMessage());
        }
    }

    private void openSellerInterface() {
        try {
            URL fxmlUrl = getClass().getResource("/views/sellerDashboard.fxml");
            if (fxmlUrl == null) {
                throw new IOException("FXML file not found in resources");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            SellerDashboardController controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Controller initialization failed");
            }

            controller.setServices(services);
            controller.setCurrentUsername(loginUsername.getText().trim());

            Stage sellerStage = new Stage();
            sellerStage.setScene(new Scene(root));
            sellerStage.setTitle("Ticket Seller Dashboard");
            sellerStage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "FXML Error",
                    "Failed to load interface", e.toString());
            e.printStackTrace(); // Check full stack trace
        }
    }
}