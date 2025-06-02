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
        logger.info("Starting REST server with WebSocket and JWT authentication support...");

        SpringApplication app = new SpringApplication(StartRestServer.class);

        // Add additional properties for WebSocket and Security
        System.setProperty("server.port", "8080");
        System.setProperty("logging.level.org.example.websocket", "DEBUG");
        System.setProperty("logging.level.org.example.security", "DEBUG");
        System.setProperty("logging.level.org.springframework.security", "DEBUG");

        app.run(args);

        logger.info("REST server started successfully with:");
        logger.info("- WebSocket endpoint: ws://localhost:8080/ws/matches");
        logger.info("- REST API: http://localhost:8080/api/matches");
        logger.info("- Authentication: http://localhost:8080/api/auth/*");
        logger.info("- Web Client: Open basketball-web-client.html in browser");
    }
}