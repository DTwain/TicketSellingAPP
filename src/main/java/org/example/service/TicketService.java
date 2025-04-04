package org.example.service;

import org.example.domain.*;
import org.example.repository.RepositoryException;
import org.example.repository.interfaces.MatchInterface;
import org.example.repository.interfaces.TicketInterface;
import org.example.repository.interfaces.UserInterface;
import org.example.service.interfaces.TicketServiceInterface;
import org.example.utils.events.ChangeEventType;
import org.example.utils.events.EntityChangeType;
import org.example.utils.observer.Observable;
import org.example.utils.observer.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TicketService implements TicketServiceInterface  {
    private List<Observer<EntityChangeType<?>>> observers;
    private TicketInterface ticketInterface;
    private UserInterface userInterface;
    private MatchInterface matchInterface;

    public TicketService(TicketInterface ticketInterface, MatchInterface matchInterface, UserInterface userInterface) {
        this.ticketInterface = ticketInterface;
        this.matchInterface = matchInterface;
        this.userInterface = userInterface;
        observers = new ArrayList<>();
    }

    @Override
    public int countAvailableTicketsByMatch(Long matchId) throws ServicesException {
        Optional<Match> matchOptional = Optional.empty();
        try {
            matchOptional = matchInterface.findOne(matchId);
        } catch (RepositoryException | IllegalArgumentException e) {
            throw new ServicesException(e);
        }

        if(matchOptional.isEmpty())
            throw new ServicesException("No match with provided id");

        try {
            return ticketInterface.countAvailableTicketsByMatch(matchId);
        } catch (RepositoryException e) {
            throw new ServicesException(e);
        }
    }

    @Override
    public String ticketPriceRangePerMatch(Long matchId) throws ServicesException {
        Optional<Match> matchOptional = Optional.empty();
        try {
            matchOptional = matchInterface.findOne(matchId);
        } catch (RepositoryException | IllegalArgumentException e) {
            throw new ServicesException(e);
        }

        if(matchOptional.isEmpty())
            throw new ServicesException("No match with provided id");

        try {
            return ticketInterface.ticketPriceRangeByMatch(matchId);
        } catch (RepositoryException e) {
            throw new ServicesException(e);
        }
    }

    @Override
    public Double sellTickets(TicketSale ticketSale) throws ServicesException{
        Optional<Match> matchOptional = Optional.empty();
        try {
            matchOptional = matchInterface.findOne(ticketSale.getMatchId());
        } catch (RepositoryException | IllegalArgumentException e) {
            throw new ServicesException(e);
        }

        if(matchOptional.isEmpty())
            throw new ServicesException("No match with provided id");

        Optional<User> userOptional = Optional.empty();
        try {
            userOptional = userInterface.findUserByUsername(ticketSale.getCustomerName());
        } catch (RepositoryException | IllegalArgumentException e) {
            throw new ServicesException(e);
        }

        if(userOptional.isEmpty())
            throw new ServicesException("No match with provided username");


        try {
            Double totalPrice =  ticketInterface.sellTickets(ticketSale, userOptional.get());
            notifyObservers(new EntityChangeType<>(ChangeEventType.UPDATE, matchOptional.get()));
            return totalPrice;
        } catch (RepositoryException e) {
            throw new ServicesException(e);
        }
    }

    @Override
    public List<Ticket> findTicketsBoughtByUser(User user) throws ServicesException {
        try {
            List<Ticket> ticketList = ticketInterface.findTicketsBoughtByUser(user);
            for(Ticket ticket : ticketList) {
                Optional<Match> matchOptional = matchInterface.findOne(ticket.getMatch().getId());
                matchOptional.ifPresent(ticket::setMatch);
            }
            return ticketList;

        } catch (RepositoryException e) {
            throw new ServicesException(e);
        }
    }

    @Override
    public List<Ticket> findTicketsByCustomer(String name) throws ServicesException {
        Optional<User> userOptional = Optional.empty();
        try {
            userOptional = userInterface.findUserByUsername(name);
        } catch (RepositoryException | IllegalArgumentException e) {
            throw new ServicesException(e);
        }

        if(userOptional.isEmpty())
            throw new ServicesException("No match with provided username");

        return findTicketsBoughtByUser(userOptional.get());
    }

    @Override
    public void addObserver(Observer<EntityChangeType<?>> e) {
        observers.add(e);
    }

    @Override
    public void removeObserver(Observer<EntityChangeType<?>> e) {
        observers.remove(e);
    }

    @Override
    public void notifyObservers(EntityChangeType<?> t) {
        for (Observer<EntityChangeType<?>> observer : observers) {
            observer.update(t);
        }
    }

}
