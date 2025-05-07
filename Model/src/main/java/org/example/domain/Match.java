package org.example.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Match extends Entity<Long> {
    private String teamA;
    private String teamB;
    private LocalDateTime dateTime;
    private transient Integer availableTickets;
    private transient String priceRange;

    public Match() {}

    public Match(Long id, String teamA, String teamB, LocalDateTime dateTime) {
        setId(id);
        this.teamA = teamA;
        this.teamB = teamB;
        this.dateTime = dateTime;
    }

    public String getTeamA() {
        return teamA;
    }

    public void setTeamA(String teamA) {
        this.teamA = teamA;
    }

    public String getTeamB() {
        return teamB;
    }

    public void setTeamB(String teamB) {
        this.teamB = teamB;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getMatchDescription() {
        return teamA + " vs " + teamB;
    }

    public String getFormattedDate() {
        return this.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    // Dummy property for action column
    public String getDummy() {
        return "";
    }

    public void setAvailableTickets(Integer availableTickets) {
        this.availableTickets = availableTickets;
    }

    public Integer getAvailableTickets() {
        return availableTickets;
    }

    public String getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(String priceRange) {
        this.priceRange = priceRange;
    }
}