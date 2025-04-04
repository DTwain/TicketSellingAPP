package org.example.service;

import org.example.domain.User;
import org.example.repository.MatchRepository;
import org.example.repository.UserRepository;
import org.example.repository.interfaces.TicketInterface;
import org.example.repository.interfaces.UserInterface;
import org.example.service.interfaces.UserServiceInterface;

import java.util.Optional;

public class UserService implements UserServiceInterface {
    private UserInterface userInterface;
    private TicketInterface ticketInterface;
    public UserService(UserInterface userInterface, TicketInterface ticketInterface) {
        this.userInterface = userInterface;
        this.ticketInterface = ticketInterface;
    }


    @Override
    public boolean authenticate(String username, String password) throws ServicesException {
        try {
            // Find user by username (you might need to implement this method in your repository)
            Optional<User> userOpt = userInterface.findUserByUsername(username);
            if (userOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            // In a real application, you should use password hashing (e.g., BCrypt)
            return user.getPassword().equals(password);
        } catch (Exception e) {
            throw new ServicesException("Authentication failed", e);
        }
    }

    @Override
    public boolean usernameExists(String username) throws ServicesException {
        try {
            Optional<User> userOpt = userInterface.findUserByUsername(username);
            return userOpt.isPresent();
        } catch (Exception e) {
            throw new ServicesException("Error checking username existence", e);
        }
    }

    @Override
    public boolean registerUser(String username, String password) throws ServicesException {
        try {
            // Check if username already exists
            if (usernameExists(username)) {
                return false;
            }

            // Create new user
            // Note: In a real application, you should hash the password before storing it
            User newUser = new User(username, password); // Assuming your User constructor

            // Save the user
            Optional<User> savedUser = userInterface.save(newUser);
            return savedUser.isEmpty();
        } catch (Exception e) {
            throw new ServicesException("Registration failed", e);
        }
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws ServicesException {
        try {
            return userInterface.findUserByUsername(username);
        } catch (Exception e) {
            throw new ServicesException("Error checking username existence", e);
        }
    }

}
