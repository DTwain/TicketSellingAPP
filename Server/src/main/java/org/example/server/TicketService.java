package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.persistence.RepositoryException;
import org.example.persistence.interfaces.MatchInterface;
import org.example.persistence.interfaces.TicketInterface;
import org.example.persistence.interfaces.UserInterface;
import org.example.service.ServicesException;
import org.example.service.interfaces.TicketServiceInterface;

import org.example.utils.observer.TicketObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.List;
import java.util.Optional;

public class TicketService implements TicketServiceInterface {
    private final TicketInterface ticketRepository;
    private final MatchInterface matchRepository;
    private final UserInterface userRepository;
    private static final Logger logger = LogManager.getLogger(TicketService.class);
    private Map<Long, TicketObserver> registeredObservers = new ConcurrentHashMap<>();

    public TicketService(TicketInterface ticketRepository, MatchInterface matchRepository, UserInterface userRepository) {
        this.ticketRepository = ticketRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        logger.info("TicketService initialized");
    }

    @Override
    public int countAvailableTicketsByMatch(Long matchId) throws ServicesException {
        try {
            Optional<Match> matchOptional = matchRepository.findOne(matchId);
            if (matchOptional.isEmpty()) {
                throw new ServicesException("Match not found");
            }

            return ticketRepository.countAvailableTicketsByMatch(matchId);
        } catch (RepositoryException e) {
            logger.error("Error counting available tickets for match {}", matchId, e);
            throw new ServicesException("Error counting available tickets", e);
        }
    }

    @Override
    public String ticketPriceRangePerMatch(Long matchId) throws ServicesException {
        try {
            Optional<Match> matchOptional = matchRepository.findOne(matchId);
            if (matchOptional.isEmpty()) {
                throw new ServicesException("Match not found");
            }

            return ticketRepository.ticketPriceRangeByMatch(matchId);
        } catch (RepositoryException e) {
            logger.error("Error getting price range for match {}", matchId, e);
            throw new ServicesException("Error retrieving price range", e);
        }
    }

    @Override
    public Double sellTickets(TicketSale ticketSale) throws ServicesException {
        try {
            Optional<Match> matchOptional = matchRepository.findOne(ticketSale.getMatchId());
            if (matchOptional.isEmpty()) {
                throw new ServicesException("Match not found");
            }

            Optional<User> userOptional = userRepository.findUserByUsername(ticketSale.getCustomerName());
            if (userOptional.isEmpty()) {
                throw new ServicesException("User not found");
            }

            Double totalPrice = ticketRepository.sellTickets(ticketSale, userOptional.get());

            Match match = matchOptional.get();
            User buyer = userOptional.get();

            notifyMatchUpdated(match);

            // Notify the buyer about their tickets
            notifyUserTicketsChanged(buyer);

            return totalPrice;

        } catch (RepositoryException e) {
            logger.error("Error selling tickets for match {}", ticketSale.getMatchId(), e);
            throw new ServicesException("Error selling tickets", e);
        }
    }

    @Override
    public List<Ticket> findTicketsBoughtByUser(User user) throws ServicesException {
        try {
            return ticketRepository.findTicketsBoughtByUser(user);
        } catch (RepositoryException e) {
            logger.error("Error finding tickets for user {}", user.getUsername(), e);
            throw new ServicesException("Error retrieving user tickets", e);
        }
    }

    @Override
    public List<Ticket> findTicketsByCustomer(String name) throws ServicesException {
        try {
            return ticketRepository.findTicketsByCustomer(name);
        } catch (RepositoryException e) {
            logger.error("Error finding tickets for customer {}", name, e);
            throw new ServicesException("Error retrieving customer tickets", e);
        }
    }

    @Override
    public void registerObserver(User user, TicketObserver client) throws ServicesException {
        registeredObservers.put(user.getId(), client);
    }

    @Override
    public void unregisterObserver(User user) throws ServicesException {
        registeredObservers.remove(user.getId());
    }

    private void notifyMatchUpdated(Match match) {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (Map.Entry<Long, TicketObserver> entry : registeredObservers.entrySet()) {
            TicketObserver observer = entry.getValue();
            if (observer != null) {
                executor.execute(() -> {
                    try {
                        observer.matchUpdated(match);
                    } catch (ServicesException e) {
                        logger.error("Error notifying about match update", e);
                    }
                });
            }
        }

        executor.shutdown();
    }

    private void notifyUserTicketsChanged(User user) {
        TicketObserver observer = registeredObservers.get(user.getId());
        if (observer != null) {
            try {
                observer.userTicketsChanged(user);
            } catch (ServicesException e) {
                logger.error("Error notifying user about ticket change", e);
            }
        }
    }
}