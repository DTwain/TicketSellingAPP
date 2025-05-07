package org.example.domain;

import java.util.Set;

public class User extends Entity<Long> {
    private String username;
    private String password;
    private Set<Ticket> boughtTickets;

    public User(Long id, String username, String password) {
        setId(id);
        this.username = username;
        this.password = password;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Ticket> getBoughtTickets() {
        return boughtTickets;
    }

    public void setBoughtTickets(Set<Ticket> boughtTickets) {
        this.boughtTickets = boughtTickets;
    }

    public void addBoughtTicketToUser(Ticket ticket) {
        boughtTickets.add(ticket);
    }
}