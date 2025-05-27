package org.example.persistence.entities;

import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.User;

import javax.persistence.*;
import java.util.Optional;

@Entity
@Table(name = "Ticket")
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "seatNumber", nullable = false)
    private Integer seatNumber;

    @Column(name = "sold", nullable = false)
    private Integer sold; // SQLite uses INTEGER for boolean

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "user_id")
    private Long userId;

    // Constructors
    public TicketEntity() {}

    public TicketEntity(Long matchId, Integer seatNumber, Boolean sold, Double price, Long userId) {
        this.matchId = matchId;
        this.seatNumber = seatNumber;
        this.sold = sold ? 1 : 0;
        this.price = price;
        this.userId = userId;
    }

    // Convert to domain object (requires additional data)
    public Ticket toDomain(Match match, Optional<User> user) {
        return new Ticket(id, match, seatNumber, sold == 1, price, user);
    }

    // Create from domain object
    public static TicketEntity fromDomain(Ticket ticket) {
        TicketEntity entity = new TicketEntity();
        entity.setId(ticket.getId());
        entity.setMatchId(ticket.getMatch() != null ? ticket.getMatch().getId() : null);
        entity.setSeatNumber(ticket.getSeatNumber());
        entity.setSold(ticket.isSold() ? 1 : 0);
        entity.setPrice(ticket.getPrice());
        entity.setUserId(ticket.getUser().isPresent() ? ticket.getUser().get().getId() : null);
        return entity;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getSold() {
        return sold;
    }

    public void setSold(Integer sold) {
        this.sold = sold;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isSold() {
        return sold != null && sold == 1;
    }
}