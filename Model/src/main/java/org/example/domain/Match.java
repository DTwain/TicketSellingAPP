package org.example.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Match extends Entity<Long> {
    private String teamA;
    private String teamB;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
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
        if (this.dateTime == null) {
            return "TBD"; // or "Not scheduled" or whatever you prefer
        }
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

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}