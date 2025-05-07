package org.example.network.dto;

import java.io.Serializable;

public class MatchDTO implements Serializable {
    private Long id;
    private String teamA;
    private String teamB;
    private String dateTime;  // ISO-8601 format
    private Integer availableTickets;
    private String priceRange;

    public MatchDTO() {}

    public MatchDTO(Long id, String teamA, String teamB, String dateTime) {
        this.id = id;
        this.teamA = teamA;
        this.teamB = teamB;
        this.dateTime = dateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public Integer getAvailableTickets() {
        return availableTickets;
    }

    public void setAvailableTickets(Integer availableTickets) {
        this.availableTickets = availableTickets;
    }

    public String getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(String priceRange) {
        this.priceRange = priceRange;
    }
}