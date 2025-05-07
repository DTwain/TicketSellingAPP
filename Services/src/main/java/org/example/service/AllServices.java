package org.example.service;

import org.example.service.interfaces.MatchServiceInterface;
import org.example.service.interfaces.TicketSellerServiceInterface;
import org.example.service.interfaces.TicketServiceInterface;
import org.example.service.interfaces.UserServiceInterface;

public class AllServices {
    private MatchServiceInterface matchService;
    private TicketServiceInterface ticketService;
    private UserServiceInterface userService;
    private TicketSellerServiceInterface ticketSellerService;

    public AllServices(MatchServiceInterface matchService1, TicketServiceInterface ticketService1,
                       UserServiceInterface userService1, TicketSellerServiceInterface ticketSellerService1) {
        this.matchService = matchService1;
        this.ticketService = ticketService1;
        this.userService = userService1;
        this.ticketSellerService = ticketSellerService1;
    }

    public MatchServiceInterface getMatchService() {
        return matchService;
    }

    public TicketServiceInterface getTicketService() {
        return ticketService;
    }

    public UserServiceInterface getUserService() {
        return userService;
    }

    public TicketSellerServiceInterface getTicketSellerService() {
        return ticketSellerService;
    }
}