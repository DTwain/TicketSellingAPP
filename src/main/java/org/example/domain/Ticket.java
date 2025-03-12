package org.example.domain;

public class Ticket extends Entity<Long>{
    private Match match;
    private int seatNumber;
    private boolean sold;
    private double price;

    public Ticket() {}

    public Ticket(Long id, Match match, int seatNumber, boolean sold, double price) {
        setId(id);
        this.match = match;
        this.seatNumber = seatNumber;
        this.sold = sold;
        this.price = price;
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
}
