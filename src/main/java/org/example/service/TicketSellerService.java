package org.example.service;

import org.example.domain.User;
import org.example.repository.TicketSellerRepository;
import org.example.repository.UserRepository;
import org.example.repository.interfaces.TicketInterface;
import org.example.repository.interfaces.TicketSellerRepoInterface;
import org.example.repository.interfaces.UserInterface;
import org.example.service.ServicesException;
import org.example.service.interfaces.TicketSellerServiceInterface;

import java.util.Optional;

public class TicketSellerService implements TicketSellerServiceInterface {
    private final TicketSellerRepoInterface ticketSellerRepository;
    private final UserInterface userRepository;

    public TicketSellerService(TicketSellerRepoInterface ticketSellerRepository,
                               UserInterface userRepository) {
        this.ticketSellerRepository = ticketSellerRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean isTicketSeller(String username) throws ServicesException {
        try {
            return ticketSellerRepository.isTicketSeller(username);
        } catch (Exception e) {
            throw new ServicesException("Failed to check ticket seller status", e);
        }
    }

    @Override
    public boolean authenticate(String username, String password) throws ServicesException {
        try {
            // First check if user exists and credentials are valid
            Optional<User> userOpt = userRepository.findUserByUsername(username);
            if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
                return false;
            }

            // Then check if user is a ticket seller
            return isTicketSeller(username);
        } catch (Exception e) {
            throw new ServicesException("Authentication failed", e);
        }
    }
}