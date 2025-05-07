package org.example.persistence.interfaces;

import org.example.domain.TicketSeller;
import org.example.persistence.Repository;

import java.util.Optional;

public interface TicketSellerRepoInterface extends Repository<Long, TicketSeller> {
    boolean isTicketSeller(String username);
    Optional<TicketSeller> findByUsername(String username);
}