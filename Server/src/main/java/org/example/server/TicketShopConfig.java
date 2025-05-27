package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.persistence.hibernate.HibernateMatchRepository;
import org.example.persistence.hibernate.HibernateTicketRepository;
import org.example.persistence.interfaces.MatchInterface;
import org.example.persistence.interfaces.TicketInterface;
import org.example.persistence.interfaces.TicketSellerRepoInterface;
import org.example.persistence.interfaces.UserInterface;
import org.example.persistence.jdbc.MatchRepository;
import org.example.persistence.jdbc.TicketRepository;
import org.example.persistence.jdbc.TicketSellerRepository;
import org.example.persistence.jdbc.UserRepository;
import org.example.service.AllServices;
import org.example.service.interfaces.MatchServiceInterface;
import org.example.service.interfaces.TicketSellerServiceInterface;
import org.example.service.interfaces.TicketServiceInterface;
import org.example.service.interfaces.UserServiceInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class TicketShopConfig {
    private static final Logger logger = LogManager.getLogger(TicketShopConfig.class);

    @Bean
    public Properties getProperties() {
        Properties properties = new Properties();
        try {
            properties.load(TicketShopConfig.class.getResourceAsStream("/bd.config"));
            logger.info("Database configuration loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load bd.config", e);
            throw new RuntimeException("Configuration file bd.config not found", e);
        }
        return properties;
    }

    @Bean
    public MatchInterface matchRepository() {
        // Choose between JDBC and Hibernate implementation
        boolean useHibernate = Boolean.parseBoolean(
                getProperties().getProperty("use.hibernate", "false")
        );

        if (useHibernate) {
            return new HibernateMatchRepository(getProperties());
        } else {
            return new MatchRepository(getProperties());
        }
    }

    @Bean
    public TicketInterface ticketRepository() {
        // Choose between JDBC and Hibernate implementation
        boolean useHibernate = Boolean.parseBoolean(
                getProperties().getProperty("use.hibernate", "false")
        );

        if (useHibernate) {
            return new HibernateTicketRepository(getProperties(), matchRepository());
        } else {
            return new TicketRepository(getProperties(), matchRepository());
        }
    }

    @Bean
    public UserInterface userRepository() {
        return new UserRepository(getProperties());
    }

    @Bean
    public TicketSellerRepoInterface ticketSellerRepository() {
        return new TicketSellerRepository(getProperties());
    }

    @Bean
    public MatchServiceInterface matchService() {
        return new MatchService(matchRepository());
    }

    @Bean
    public TicketServiceInterface ticketService() {
        return new TicketService(ticketRepository(), matchRepository(), userRepository());
    }

    @Bean
    public UserServiceInterface userService() {
        return new UserService(userRepository(), ticketSellerRepository());
    }

    @Bean
    public TicketSellerServiceInterface ticketSellerService() {
        return new TicketSellerService(ticketSellerRepository(), userRepository());
    }

    @Bean
    public AllServices allServices() {
        return new AllServices(matchService(), ticketService(), userService(), ticketSellerService());
    }
}