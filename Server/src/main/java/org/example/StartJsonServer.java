package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.network.utils.BasketballJsonServer;
import org.example.network.utils.ServerException;
import org.example.server.TicketShopConfig;
import org.example.service.AllServices;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.Properties;

public class StartJsonServer {
    private static final int DEFAULT_PORT = 55556;
    private static final Logger logger = LogManager.getLogger(StartJsonServer.class);

    public static void main(String[] args) {
        Properties serverProps = new Properties();
        try {
            serverProps.load(StartJsonServer.class.getResourceAsStream("/server.properties"));
            logger.info("Server properties loaded");
        } catch (IOException e) {
            logger.error("Cannot find server.properties", e);
            logger.info("Using default port: {}", DEFAULT_PORT);
            serverProps.setProperty("server.port", String.valueOf(DEFAULT_PORT));
        }

        // Create Spring context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TicketShopConfig.class);
        AllServices services = context.getBean(AllServices.class);

        int port = DEFAULT_PORT;
        try {
            port = Integer.parseInt(serverProps.getProperty("server.port"));
        } catch (NumberFormatException e) {
            logger.error("Invalid port number, using default: {}", DEFAULT_PORT);
        }

        logger.info("Starting server on port: {}", port);
        BasketballJsonServer server = new BasketballJsonServer(port, services);

        try {
            server.start();
        } catch (ServerException e) {
            logger.error("Error starting server", e);
        } finally {
            context.close();
        }
    }
}