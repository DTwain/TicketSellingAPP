package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.server.TicketShopConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@Import(TicketShopConfig.class)
@ComponentScan(basePackages = {
        "org.example.rest",
        "org.example.config",
        "org.example.websocket",
        "org.example.security"
})
public class StartRestServer {
    private static final Logger logger = LogManager.getLogger(StartRestServer.class);

    public static void main(String[] args) {
        logger.info("Starting REST server with WebSocket support...");

        SpringApplication app = new SpringApplication(StartRestServer.class);

        // Add additional properties for WebSocket
        System.setProperty("server.port", "8080");
        System.setProperty("logging.level.org.example.websocket", "DEBUG");

        app.run(args);

        logger.info("REST server with WebSocket support started successfully");
        logger.info("WebSocket endpoint available at: ws://localhost:8080/ws/matches");
        logger.info("REST API available at: http://localhost:8080/api/matches");
    }
}