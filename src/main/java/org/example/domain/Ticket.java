package org.example.domain;

import java.util.Optional;

public class Ticket extends Entity<Long>{
    private Match match;
    private int seatNumber;
    private boolean sold;
    private double price;
    private Optional<User> user;


    public Ticket() {}

    public Ticket(Long id, Match match, int seatNumber, boolean sold, double price, Optional<User> user1) {
        setId(id);
        this.match = match;
        this.seatNumber = seatNumber;
        this.sold = sold;
        this.price = price;
        this.user = user1;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
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

    public Optional<User> getUser() {
        return user;
    }

    public void setUser(Optional<User> user) {
        this.user = user;
    }

    public String getMatchDescription() {
        return match != null ? match.getMatchDescription() : "";
    }

    public String getMatchDate() {
        return match != null ? match.getFormattedDate() : "";
    }

    public String getCustomerName() {
        if(user.isEmpty()) return "";
        else return user.get().getUsername();
    }

    public String getCustomerAddress(){
        return "...";
    }



}
