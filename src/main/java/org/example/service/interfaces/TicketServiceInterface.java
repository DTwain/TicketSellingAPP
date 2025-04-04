package org.example.service.interfaces;

import org.example.domain.*;
import org.example.service.ServicesException;
import org.example.utils.events.EntityChangeType;
import org.example.utils.observer.Observable;

import java.util.List;

public interface TicketServiceInterface extends Observable<EntityChangeType<?>> {
    public int countAvailableTicketsByMatch(Long matchId) throws ServicesException;
    public String ticketPriceRangePerMatch(Long matchId) throws ServicesException;

    Double sellTickets(TicketSale ticketSale) throws ServicesException;

    List<Ticket> findTicketsBoughtByUser(User user) throws ServicesException;

    List<Ticket> findTicketsByCustomer(String name) throws ServicesException;
}
