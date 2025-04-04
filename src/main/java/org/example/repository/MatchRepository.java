package org.example.repository;

import org.example.domain.Match;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.repository.interfaces.MatchInterface;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class MatchRepository implements MatchInterface {
    private JdbcUtils jdbcUtils;
    private static final Logger log = LogManager.getLogger(MatchRepository.class);

    public MatchRepository(Properties properties) {
        log.info("Initializing MatchDbRepository...");
        jdbcUtils = new JdbcUtils(properties);
    }

    @Override
    public Optional<Match> findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String sql = "SELECT id, teamA, teamB, dateTime FROM Match WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long matchId = rs.getLong("id");
                    String teamA = rs.getString("teamA");
                    String teamB = rs.getString("teamB");
                    String dtStr = rs.getString("dateTime");
                    LocalDateTime dateTime = LocalDateTime.parse(dtStr, formatter);
                    return Optional.of(new Match(matchId, teamA, teamB, dateTime));
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Connection connection = jdbcUtils.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Long id = null;
                String teamA = null;
                String teamB = null;
                String dtStr = null;
                try {
                    id = rs.getLong("id");
                    teamA = rs.getString("teamA");
                    teamB = rs.getString("teamB");
                    dtStr = rs.getString("dateTime");

                    // Handle NULL values
                    if (dtStr == null) {
                        matches.add(new Match(id, teamA, teamB, null));
                        continue;
                    }

                    // Parse with the specific format
                    LocalDateTime dateTime = LocalDateTime.parse(dtStr, formatter);
                    matches.add(new Match(id, teamA, teamB, dateTime));

                } catch (DateTimeParseException e) {
                    log.error("Failed to parse dateTime '{}' for match ID {}", dtStr, id, e);
                    // Optionally: add with null date or current time
                    matches.add(new Match(id, teamA, teamB, null));
                }
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
        String sql = "INSERT INTO Match (teamA, teamB, dateTime) VALUES (?, ?, ?)";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, entity.getTeamA());
            stmt.setString(2, entity.getTeamB());
            stmt.setString(3, entity.getDateTime().toString());

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
        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

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

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

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