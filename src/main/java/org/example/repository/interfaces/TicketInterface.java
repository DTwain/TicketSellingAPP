package org.example.repository.interfaces;

import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.repository.Repository;
import org.example.repository.RepositoryException;
import org.example.service.ServicesException;

import java.util.List;

public interface TicketInterface extends Repository<Long, Ticket> {
    public int countAvailableTicketsByMatch(Long matchId) throws RepositoryException;
    public String ticketPriceRangeByMatch(Long matchId) throws RepositoryException;
    public Double sellTickets(TicketSale ticketSale, User customer) throws RepositoryException;
    List<Ticket> findTicketsBoughtByUser(User user) throws RepositoryException;
}
