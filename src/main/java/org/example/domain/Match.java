package org.example.domain;

import java.time.LocalDateTime;

public class Match extends Entity<Long>{
    private String teamA;
    private String teamB;
    private LocalDateTime dateTime;

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

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}
