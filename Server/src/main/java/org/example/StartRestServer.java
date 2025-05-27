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
public class StartRestServer {
    private static final Logger logger = LogManager.getLogger(StartRestServer.class);

    public static void main(String[] args) {
        logger.info("Starting REST server...");
        SpringApplication.run(StartRestServer.class, args);
        logger.info("REST server started successfully");
    }
}