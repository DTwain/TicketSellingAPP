package org.example.service.interfaces;

import org.example.domain.UserType;
import org.example.service.ServicesException;

public interface TicketSellerServiceInterface {
    boolean isTicketSeller(String username) throws ServicesException;
    UserType authenticate(String username, String password) throws ServicesException;
}