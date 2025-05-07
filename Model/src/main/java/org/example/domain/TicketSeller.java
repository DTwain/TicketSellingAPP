package org.example.domain;

public class TicketSeller extends Entity<Long> {
    private String username;

    public TicketSeller() {}

    public TicketSeller(Long id, String username) {
        setId(id);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}