package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.User;
import org.example.domain.UserType;
import org.example.persistence.RepositoryException;
import org.example.persistence.interfaces.TicketSellerRepoInterface;
import org.example.persistence.interfaces.UserInterface;
import org.example.service.ServicesException;
import org.example.service.interfaces.UserServiceInterface;

import java.util.Optional;

public class UserService implements UserServiceInterface {
    private final UserInterface userRepository;
    private final TicketSellerRepoInterface ticketSellerRepository;
    private static final Logger logger = LogManager.getLogger(UserService.class);

    public UserService(UserInterface userRepository, TicketSellerRepoInterface ticketSellerRepository) {
        this.userRepository = userRepository;
        this.ticketSellerRepository = ticketSellerRepository;
        logger.info("UserService initialized");
    }

    @Override
    public UserType authenticate(String username, String password) throws ServicesException {
        try {
            Optional<User> userOptional = userRepository.findUserByUsername(username);
            if (userOptional.isEmpty() || !userOptional.get().getPassword().equals(password)) {
                logger.info("Authentication failed for user: {}", username);
                return UserType.UNKNOWN;
            }

            // Check if the user is a ticket seller
            if (ticketSellerRepository.isTicketSeller(username)) {
                logger.info("User {} authenticated as a ticket seller", username);
                return UserType.SELLER;
            } else {
                logger.info("User {} authenticated as a regular user", username);
                return UserType.USER;
            }
        } catch (RepositoryException e) {
            logger.error("Error during authentication for user {}", username, e);
            throw new ServicesException("Authentication error", e);
        }
    }

    @Override
    public boolean usernameExists(String username) throws ServicesException {
        try {
            return userRepository.findUserByUsername(username).isPresent();
        } catch (RepositoryException e) {
            logger.error("Error checking if username exists: {}", username, e);
            throw new ServicesException("Error checking username", e);
        }
    }

    @Override
    public boolean registerUser(String username, String password) throws ServicesException {
        try {
            if (usernameExists(username)) {
                logger.info("Registration failed - username already exists: {}", username);
                return false;
            }

            User newUser = new User(username, password);
            Optional<User> result = userRepository.save(newUser);

            boolean success = result.isEmpty(); // If empty, save was successful
            logger.info("User registration {}: {}", success ? "successful" : "failed", username);
            return success;
        } catch (RepositoryException e) {
            logger.error("Error registering user: {}", username, e);
            throw new ServicesException("Error registering user", e);
        }
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws ServicesException {
        try {
            return userRepository.findUserByUsername(username);
        } catch (RepositoryException e) {
            logger.error("Error finding user by username: {}", username, e);
            throw new ServicesException("Error retrieving user", e);
        }
    }

    @Override
    public Optional<User> getUserById(Long id) throws ServicesException {
        try {
            return userRepository.findOne(id);
        } catch (RepositoryException e) {
            logger.error("Error fetching user by id: {}", id, e);
            throw new ServicesException("Error retrieving user", e);
        }
    }
}