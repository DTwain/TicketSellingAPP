package org.example.repository.interfaces;

import org.example.domain.TicketSeller;
import org.example.repository.Repository;

import java.util.Optional;

public interface TicketSellerRepoInterface extends Repository<Long, TicketSeller> {
    public boolean isTicketSeller(String username);
    public Optional<TicketSeller> findByUsername(String username);
}
