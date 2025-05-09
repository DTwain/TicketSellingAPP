package org.example.utils.observer;

import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.User;
import org.example.service.ServicesException;

public interface TicketObserver {
    void ticketSold(Ticket ticket) throws ServicesException;
    void matchUpdated(Match match) throws ServicesException;
    void userTicketsChanged(User user) throws ServicesException;
}