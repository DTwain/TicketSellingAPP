package org.example.persistence.interfaces;

import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.persistence.Repository;
import org.example.persistence.RepositoryException;

import java.util.List;

public interface TicketInterface extends Repository<Long, Ticket> {
    int countAvailableTicketsByMatch(Long matchId) throws RepositoryException;
    String ticketPriceRangeByMatch(Long matchId) throws RepositoryException;
    Double sellTickets(TicketSale ticketSale, User customer) throws RepositoryException;
    List<Ticket> findTicketsBoughtByUser(User user) throws RepositoryException;
    List<Ticket> findTicketsByCustomer(String name) throws RepositoryException;
}