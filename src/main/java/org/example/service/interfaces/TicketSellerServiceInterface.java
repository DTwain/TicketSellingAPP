package org.example.service.interfaces;

import org.example.service.ServicesException;

public interface TicketSellerServiceInterface {
    public boolean isTicketSeller(String username) throws ServicesException;
    public boolean authenticate(String username, String password) throws ServicesException;
}
