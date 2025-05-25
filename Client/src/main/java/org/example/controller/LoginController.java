package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.User;
import org.example.domain.UserType;
import org.example.network.grpc.BasketballGrpcServicesProxy;
import org.example.service.ServicesException;

import java.util.Optional;

public class LoginController {
    @FXML private VBox loginForm;
    @FXML private VBox signupForm;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private TextField signupUsername;
    @FXML private PasswordField signupPassword;
    @FXML private PasswordField signupConfirmPassword;

    private BasketballGrpcServicesProxy basketballServicesProxy;
    private Parent clientView;
    private Parent sellerView;
    private ClientDashboardController clientController;
    private SellerDashboardController sellerController;

    private static final Logger logger = LogManager.getLogger(LoginController.class);

    @FXML
    private void initialize() {
        // Ensure signup form is initially hidden
        if (signupForm != null) {
            signupForm.setVisible(false);
            signupForm.setManaged(false);
        }
    }

    public void setServices(BasketballGrpcServicesProxy basketballServicesProxy) {
        this.basketballServicesProxy = basketballServicesProxy;
    }

    public void setClientView(Parent view, ClientDashboardController controller) {
        this.clientView = view;
        this.clientController = controller;
    }

    public void setSellerView(Parent view, SellerDashboardController controller) {
        this.sellerView = view;
        this.sellerController = controller;
    }

    @FXML
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
    private void handleLogin(ActionEvent event) {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Failed", "Empty Fields",
                    "Please enter both username and password");
            return;
        }

        try {
            logger.info("Attempting gRPC authentication for user: {}", username);
            UserType userType = basketballServicesProxy.authenticate(username, password);

            if (userType == UserType.SELLER) {
                showAlert(Alert.AlertType.INFORMATION, "Login Success", "Welcome Seller",
                        "You have successfully logged in as a ticket seller!");
                openSellerInterface(username);
            } else if (userType == UserType.USER) {
                showAlert(Alert.AlertType.INFORMATION, "Login Success", "Welcome",
                        "You have successfully logged in!");
                openClientInterface(username);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Credentials",
                        "The username or password is incorrect");
            }
        } catch (ServicesException e) {
            logger.error("gRPC Login error", e);
            showAlert(Alert.AlertType.ERROR, "Login Error", "Connection Error",
                    "Failed to verify credentials via gRPC: " + e.getMessage());
        }
    }

    @FXML
    private void handleSignup(ActionEvent event) {
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
            logger.info("Attempting gRPC registration for user: {}", username);

            // Check if username already exists
            if (basketballServicesProxy.usernameExists(username)) {
                showAlert(Alert.AlertType.ERROR, "Signup Failed", "Username Taken",
                        "This username is already in use. Please choose another one.");
                return;
            }

            // Create new user
            boolean signupSuccess = basketballServicesProxy.registerUser(username, password);
            if (signupSuccess) {
                showAlert(Alert.AlertType.INFORMATION, "Signup Success", "Account Created",
                        "Your account has been successfully created via gRPC!");
                toggleForms(); // Switch back to login form
                clearSignupFields();
            } else {
                showAlert(Alert.AlertType.ERROR, "Signup Failed", "Registration Error",
                        "Failed to create account. Please try again.");
            }
        } catch (ServicesException e) {
            logger.error("gRPC Signup error", e);
            showAlert(Alert.AlertType.ERROR, "Signup Error", "Connection Error",
                    "Failed to register user via gRPC: " + e.getMessage());
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

    private void openClientInterface(String username) {
        try {
            logger.info("Opening client interface for user: {}", username);
            Optional<User> userOptional = basketballServicesProxy.getUserByUsername(username);
            if (userOptional.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "User data not found",
                        "Could not retrieve user information via gRPC");
                return;
            }

            Stage clientStage = new Stage();

            // Set up the controller with gRPC proxy
            clientController.setServices(basketballServicesProxy);
            clientController.setCurrentUser(userOptional.get());

            clientStage.setScene(new Scene(clientView));
            clientStage.setTitle("Client Dashboard - gRPC");
            clientStage.show();

            // Hide the login window
            ((Stage) loginUsername.getScene().getWindow()).close();

            logger.info("Client interface opened successfully for user: {}", username);
        } catch (Exception e) {
            logger.error("Error opening client interface via gRPC", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open client interface",
                    "gRPC connection error: " + e.getMessage());
        }
    }

    private void openSellerInterface(String username) {
        try {
            logger.info("Opening seller interface for user: {}", username);
            Optional<User> userOptional = basketballServicesProxy.getUserByUsername(username);
            if (userOptional.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "User data not found",
                        "Could not retrieve user information via gRPC");
                return;
            }

            Stage sellerStage = new Stage();

            // Set up the controller with gRPC proxy
            sellerController.setServices(basketballServicesProxy);
            sellerController.setCurrentUser(userOptional.get());

            sellerStage.setScene(new Scene(sellerView));
            sellerStage.setTitle("Ticket Seller Dashboard - gRPC");
            sellerStage.show();

            // Hide the login window
            ((Stage) loginUsername.getScene().getWindow()).close();

            logger.info("Seller interface opened successfully for user: {}", username);
        } catch (Exception e) {
            logger.error("Error opening seller interface via gRPC", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open seller interface",
                    "gRPC connection error: " + e.getMessage());
        }
    }
}