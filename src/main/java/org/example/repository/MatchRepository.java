package org.example.repository;

import org.example.domain.Match;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class MatchRepository implements Repository<Long, Match> {
    private static final Logger log = LogManager.getLogger(MatchRepository.class);
    private String url;

    public MatchRepository() {
        log.info("Initializing MatchDbRepository...");
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
            this.url = props.getProperty("db.url");
            log.debug("Loaded db.url = {}", this.url);
        } catch (IOException e) {
            log.error("Failed to load db.properties", e);
        }
    }

    @Override
    public Optional<Match> findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }
        String sql = "SELECT id, teamA, teamB, dateTime FROM Match WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long matchId = rs.getLong("id");
                    String teamA = rs.getString("teamA");
                    String teamB = rs.getString("teamB");
                    String dtStr = rs.getString("dateTime");
                    LocalDateTime dateTime = LocalDateTime.parse(dtStr);
                    Match match = new Match(matchId, teamA, teamB, dateTime);
                    return Optional.of(match);
                }
            }
        } catch (SQLException e) {
            log.error("Error finding Match with id {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public Iterable<Match> findAll() {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT id, teamA, teamB, dateTime FROM Match";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Long id = rs.getLong("id");
                String teamA = rs.getString("teamA");
                String teamB = rs.getString("teamB");
                String dtStr = rs.getString("dateTime");
                LocalDateTime dateTime = LocalDateTime.parse(dtStr);
                Match match = new Match(id, teamA, teamB, dateTime);
                matches.add(match);
            }
        } catch (SQLException e) {
            log.error("Error retrieving all matches", e);
        }
        return matches;
    }

    @Override
    public Optional<Match> save(Match entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        String sql = "INSERT INTO Match (id, teamA, teamB, dateTime) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, entity.getId());
            stmt.setString(2, entity.getTeamA());
            stmt.setString(3, entity.getTeamB());
            stmt.setString(4, entity.getDateTime().toString());

            int rows = stmt.executeUpdate();
            log.info("Inserted {} row(s) into Match table for match id={}.", rows, entity.getId());
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Error saving Match: {}", entity, e);
            return Optional.of(entity);
        }
    }

    @Override
    public Optional<Match> delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }
        Optional<Match> matchOpt = findOne(id);
        if (matchOpt.isEmpty()) {
            return Optional.empty();
        }
        String sql = "DELETE FROM Match WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            log.info("Deleted {} row(s) from Match table for match id={}.", rows, id);
        } catch (SQLException e) {
            log.error("Error deleting Match with id {}", id, e);
        }
        return matchOpt;
    }

    @Override
    public Optional<Match> update(Match entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        String sql = "UPDATE Match SET teamA = ?, teamB = ?, dateTime = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entity.getTeamA());
            stmt.setString(2, entity.getTeamB());
            stmt.setString(3, entity.getDateTime().toString());
            stmt.setLong(4, entity.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                log.warn("No Match record found with id {} for update.", entity.getId());
                return Optional.of(entity);
            }
            log.info("Updated {} row(s) in Match table for match id={}.", rows, entity.getId());
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Error updating Match: {}", entity, e);
            return Optional.of(entity);
        }
    }
}
