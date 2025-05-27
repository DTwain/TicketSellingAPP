package org.example.persistence.entities;

import org.example.domain.Match;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "Match")
public class MatchEntity {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "teamA", nullable = false)
    private String teamA;

    @Column(name = "teamB", nullable = false)
    private String teamB;

    @Column(name = "dateTime", nullable = false)
    private String dateTime; // Store as string to match SQLite schema

    // Constructors
    public MatchEntity() {}

    public MatchEntity(String teamA, String teamB, LocalDateTime dateTime) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.dateTime = dateTime != null ? dateTime.format(formatter) : null;
    }

    // Convert to domain object
    public Match toDomain() {
        LocalDateTime parsedDateTime = dateTime != null ? LocalDateTime.parse(dateTime, formatter) : null;
        return new Match(id, teamA, teamB, parsedDateTime);
    }

    // Create from domain object
    public static MatchEntity fromDomain(Match match) {
        MatchEntity entity = new MatchEntity();
        entity.setId(match.getId());
        entity.setTeamA(match.getTeamA());
        entity.setTeamB(match.getTeamB());
        entity.setDateTime(match.getDateTime() != null ? match.getDateTime().format(formatter) : null);
        return entity;
    }

    // Getters and Setters
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
}