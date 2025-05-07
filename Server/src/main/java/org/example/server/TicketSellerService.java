package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.User;
import org.example.domain.UserType;
import org.example.persistence.RepositoryException;
import org.example.persistence.interfaces.TicketSellerRepoInterface;
import org.example.persistence.interfaces.UserInterface;
import org.example.service.ServicesException;
import org.example.service.interfaces.TicketSellerServiceInterface;

import java.util.Optional;

public class TicketSellerService implements TicketSellerServiceInterface {
    private final TicketSellerRepoInterface ticketSellerRepository;
    private final UserInterface userRepository;
    private static final Logger logger = LogManager.getLogger(TicketSellerService.class);

    public TicketSellerService(TicketSellerRepoInterface ticketSellerRepository, UserInterface userRepository) {
        this.ticketSellerRepository = ticketSellerRepository;
        this.userRepository = userRepository;
        logger.info("TicketSellerService initialized");
    }

    @Override
    public boolean isTicketSeller(String username) throws ServicesException {
        try {
            return ticketSellerRepository.isTicketSeller(username);
        } catch (RepositoryException e) {
            logger.error("Error checking if user is a ticket seller: {}", username, e);
            throw new ServicesException("Error checking ticket seller status", e);
        }
    }

    @Override
    public UserType authenticate(String username, String password) throws ServicesException {
        try {
            // First check if user exists and credentials are valid
            Optional<User> userOpt = userRepository.findUserByUsername(username);
            if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
                return UserType.UNKNOWN;
            }

            if(isTicketSeller(username))
                return UserType.SELLER;
            else
                return UserType.USER;
        } catch (RepositoryException e) {
            logger.error("Error authenticating ticket seller: {}", username, e);
            throw new ServicesException("Authentication failed", e);
        }
    }
}