package org.example.network.dto;

import java.io.Serializable;

public class TicketDTO implements Serializable {
    private Long id;
    private Long matchId;
    private int seatNumber;
    private boolean sold;
    private double price;
    private Long userId;

    public TicketDTO() {}

    public TicketDTO(Long id, Long matchId, int seatNumber, boolean sold, double price, Long userId) {
        this.id = id;
        this.matchId = matchId;
        this.seatNumber = seatNumber;
        this.sold = sold;
        this.price = price;
        this.userId = userId;
    }

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

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}