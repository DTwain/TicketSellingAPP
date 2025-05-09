package org.example.service.interfaces;

import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.service.ServicesException;
import org.example.utils.observer.TicketObserver;

import java.util.List;

public interface TicketServiceInterface {
    int countAvailableTicketsByMatch(Long matchId) throws ServicesException;
    String ticketPriceRangePerMatch(Long matchId) throws ServicesException;
    Double sellTickets(TicketSale ticketSale) throws ServicesException;
    List<Ticket> findTicketsBoughtByUser(User user) throws ServicesException;
    List<Ticket> findTicketsByCustomer(String name) throws ServicesException;

    void registerObserver(User user, TicketObserver client) throws ServicesException;
    void unregisterObserver(User user) throws ServicesException;
}