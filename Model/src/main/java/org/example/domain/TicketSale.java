package org.example.domain;

public class TicketSale {
    private Long matchId;
    private String customerName;
    private String customerAddress;
    private int seatsPurchased;

    public TicketSale(Long matchId, String customerName, String customerAddress, int seatsPurchased) {
        this.matchId = matchId;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.seatsPurchased = seatsPurchased;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public int getSeatsPurchased() {
        return seatsPurchased;
    }

    public void setSeatsPurchased(int seatsPurchased) {
        this.seatsPurchased = seatsPurchased;
    }
}