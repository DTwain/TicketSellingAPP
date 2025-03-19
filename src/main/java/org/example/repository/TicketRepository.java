package org.example.repository;

import org.example.domain.Ticket;
import org.example.domain.Match;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TicketRepository implements Repository<Long, Ticket> {
    private static final Logger log = LogManager.getLogger(TicketRepository.class);
    private String url;

    public TicketRepository() {
        log.info("Initializing TicketRepository...");
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
    public Optional<Ticket> findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }
        String sql = "SELECT id, match_id, seatNumber, sold, price FROM Ticket WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long ticketId = rs.getLong("id");
                    Long matchId = rs.getLong("match_id");
                    int seatNumber = rs.getInt("seatNumber");
                    int soldInt = rs.getInt("sold");
                    boolean sold = soldInt != 0;
                    double price = rs.getDouble("price");
                    // Create a minimal Match object with only its id set
                    Match match = new Match();
                    match.setId(matchId);
                    Ticket ticket = new Ticket(ticketId, match, seatNumber, sold, price);
                    return Optional.of(ticket);
                }
            }
        } catch (SQLException e) {
            log.error("Error finding Ticket with id {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public Iterable<Ticket> findAll() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT id, match_id, seatNumber, sold, price FROM Ticket";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Long ticketId = rs.getLong("id");
                Long matchId = rs.getLong("match_id");
                int seatNumber = rs.getInt("seatNumber");
                int soldInt = rs.getInt("sold");
                boolean sold = soldInt != 0;
                double price = rs.getDouble("price");
                Match match = new Match();
                match.setId(matchId);
                Ticket ticket = new Ticket(ticketId, match, seatNumber, sold, price);
                tickets.add(ticket);
            }
        } catch (SQLException e) {
            log.error("Error retrieving all tickets", e);
        }
        return tickets;
    }

    @Override
    public Optional<Ticket> save(Ticket entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        String sql = "INSERT INTO Ticket (id, match_id, seatNumber, sold, price) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, entity.getId());
            stmt.setLong(2, entity.getMatch().getId());
            stmt.setInt(3, entity.getSeatNumber());
            stmt.setInt(4, entity.isSold() ? 1 : 0);
            stmt.setDouble(5, entity.getPrice());

            int rows = stmt.executeUpdate();
            log.info("Inserted {} row(s) into Ticket table for ticket id={}.", rows, entity.getId());
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Error saving Ticket: {}", entity, e);
            return Optional.of(entity);
        }
    }

    @Override
    public Optional<Ticket> delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }
        Optional<Ticket> ticketOpt = findOne(id);
        if (ticketOpt.isEmpty()) {
            return Optional.empty();
        }
        String sql = "DELETE FROM Ticket WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            log.info("Deleted {} row(s) from Ticket table for ticket id={}.", rows, id);
        } catch (SQLException e) {
            log.error("Error deleting Ticket with id {}", id, e);
        }
        return ticketOpt;
    }

    @Override
    public Optional<Ticket> update(Ticket entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        String sql = "UPDATE Ticket SET match_id = ?, seatNumber = ?, sold = ?, price = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, entity.getMatch().getId());
            stmt.setInt(2, entity.getSeatNumber());
            stmt.setInt(3, entity.isSold() ? 1 : 0);
            stmt.setDouble(4, entity.getPrice());
            stmt.setLong(5, entity.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                log.warn("No Ticket record found with id {} for update.", entity.getId());
                return Optional.of(entity);
            }
            log.info("Updated {} row(s) in Ticket table for ticket id={}.", rows, entity.getId());
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Error updating Ticket: {}", entity, e);
            return Optional.of(entity);
        }
    }
}
