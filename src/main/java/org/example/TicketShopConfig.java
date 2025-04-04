package org.example;

import org.example.repository.MatchRepository;
import org.example.repository.TicketRepository;
import org.example.repository.TicketSellerRepository;
import org.example.repository.UserRepository;
import org.example.repository.interfaces.MatchInterface;
import org.example.repository.interfaces.TicketInterface;
import org.example.repository.interfaces.TicketSellerRepoInterface;
import org.example.repository.interfaces.UserInterface;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class TicketShopConfig {
    @Bean
    Properties getProperties() {
        Properties properties = new Properties();
        try {
            // Load from classpath (src/main/resources)
            properties.load(getClass().getClassLoader().getResourceAsStream("bd.config"));
        } catch(IOException e) {
            System.err.println("Failed to load bd.config: " + e.getMessage());
            throw new RuntimeException("Configuration file bd.config not found", e);
        }
        return properties;
    }

    @Bean
    MatchInterface MatchRepo() {
        return new MatchRepository(getProperties());
    }

    @Bean
    TicketInterface TicketRepo() {
        return new TicketRepository(getProperties());
    }

    @Bean
    UserInterface UserRepo() {
        return new UserRepository(getProperties());
    }

    @Bean
    TicketSellerRepoInterface ticketSellerRepository() {
        return new TicketSellerRepository(getProperties());
    }

    @Bean
    AllServices services(UserRepository userRepository){
        return new AllServices(
                new MatchService(MatchRepo()),
                new TicketService(TicketRepo(), MatchRepo(), UserRepo()),
                new UserService(UserRepo(), TicketRepo()),
                new TicketSellerService(ticketSellerRepository(), UserRepo()));
    }

}
