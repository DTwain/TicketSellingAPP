package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.example.controller.LoginController;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.User;
import org.example.repository.MatchRepository;
import org.example.repository.TicketRepository;
import org.example.repository.UserRepository;
import org.example.repository.interfaces.MatchInterface;
import org.example.repository.interfaces.TicketInterface;
import org.example.repository.interfaces.UserInterface;
import org.example.service.AllServices;
import org.example.service.ServicesException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDateTime;
import java.util.Optional;

public class MainFX extends Application {
    private static final Log log = LogFactory.getLog(MainFX.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/loginAndSignUp.fxml"));

            LoginController loginController = new LoginController();
            loader.setController(loginController);
            loginController.setService(getService());

            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Ticket Shop");
            primaryStage.show();
        } catch (Exception e) {
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error ");
            alert.setContentText("Error while starting app "+e);
            alert.showAndWait();
        }
    }
    public static void main(String[] args) {
        launch(args);
    }

    static AllServices getService() throws ServicesException {
        ApplicationContext context = new AnnotationConfigApplicationContext(TicketShopConfig.class);
        return context.getBean(AllServices.class);
    }
}
