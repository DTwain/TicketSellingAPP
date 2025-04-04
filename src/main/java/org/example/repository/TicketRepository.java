package org.example.repository;

import org.example.domain.Ticket;
import org.example.domain.Match;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.repository.interfaces.TicketInterface;
import org.example.service.ServicesException;

import java.sql.*;
import java.util.*;

public class TicketRepository implements TicketInterface {
    private JdbcUtils jdbcUtils;
    private static final Logger log = LogManager.getLogger(TicketRepository.class);

    public TicketRepository(Properties properties) {
        log.info("Initializing TicketRepository...");
        jdbcUtils = new JdbcUtils(properties);
    }

    @Override
    public Optional<Ticket> findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }

        String sql = "SELECT id, match_id, seatNumber, sold, price, user_id FROM Ticket WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long ticketId = rs.getLong("id");
                    Long matchId = rs.getLong("match_id");
                    int seatNumber = rs.getInt("seatNumber");
                    boolean sold = rs.getInt("sold") != 0;
                    double price = rs.getDouble("price");

                    // Handle nullable user_id
                    Long userId = null;
                    Object userIdObj = rs.getObject("user_id");
                    if (userIdObj != null) {
                        userId = ((Number) userIdObj).longValue();
                    }

                    // Create minimal Match object
                    Match match = new Match();
                    match.setId(matchId);

                    // Look up User if userId exists
                    Optional<User> user = Optional.empty();

                    return Optional.of(new Ticket(ticketId, match, seatNumber, sold, price, user));
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
        String sql = "SELECT id, match_id, seatNumber, sold, price, user_id FROM Ticket";

        try (Connection connection = jdbcUtils.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Long ticketId = rs.getLong("id");
                Long matchId = rs.getLong("match_id");
                int seatNumber = rs.getInt("seatNumber");
                boolean sold = rs.getInt("sold") != 0;
                double price = rs.getDouble("price");

                // Handle nullable user_id
                Long userId = null;
                Object userIdObj = rs.getObject("user_id");
                if (userIdObj != null) {
                    userId = ((Number) userIdObj).longValue();
                }

                // Create minimal Match object
                Match match = new Match();
                match.setId(matchId);

                // Look up User if userId exists
                Optional<User> user = Optional.empty();

                tickets.add(new Ticket(ticketId, match, seatNumber, sold, price, user));
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

        String sql = "INSERT INTO Ticket (match_id, seatNumber, sold, price, user_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, entity.getMatch().getId());
            stmt.setInt(2, entity.getSeatNumber());
            stmt.setInt(3, entity.isSold() ? 1 : 0);
            stmt.setDouble(4, entity.getPrice());

            if (entity.getUser().isEmpty()) {
                stmt.setNull(5, Types.NULL);
            } else {
                stmt.setLong(5, entity.getUser().get().getId());
            }

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

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

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

        String sql = "UPDATE Ticket SET match_id = ?, seatNumber = ?, sold = ?, price = ?, user_id = ? WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, entity.getMatch().getId());
            stmt.setInt(2, entity.getSeatNumber());
            stmt.setInt(3, entity.isSold() ? 1 : 0);
            stmt.setDouble(4, entity.getPrice());

            if (entity.getUser().isEmpty()) {
                stmt.setNull(5, Types.NULL);
            } else {
                stmt.setLong(5, entity.getUser().get().getId());
            }

            stmt.setLong(6, entity.getId());

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


    @Override
    public int countAvailableTicketsByMatch(Long matchId) throws RepositoryException {
        String sql = "SELECT COUNT(*) FROM Ticket WHERE match_id = ? AND sold = 0";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, matchId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RepositoryException(String.format("Error counting available tickets. %s", e));
        }
    }

    @Override
    public String ticketPriceRangeByMatch(Long matchId) throws RepositoryException {
        String sql = "SELECT price FROM Ticket WHERE match_id = ? AND sold = 0";
        List<Double> ticketPriceList = new ArrayList<>();
        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, matchId);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                double price = rs.getDouble("price");
                ticketPriceList.add(price);
            }

            double minValue = ticketPriceList.stream()
                    .min(Comparator.naturalOrder())
                    .orElse(0.0);

            double maxValue = ticketPriceList.stream()
                    .max(Comparator.naturalOrder())
                    .orElse(0.0);

            return String.format("$%.2f - $%.2f", minValue, maxValue);
        } catch (SQLException e) {
            throw new RepositoryException(String.format("Error counting available tickets. %s", e));
        }
    }

    @Override
    public Double sellTickets(TicketSale ticketSale, User customer) throws RepositoryException {
        Connection connection = null;
        try {
            connection = jdbcUtils.getConnection();
            connection.setAutoCommit(false); // Start transaction

            // 1. Get available tickets for this match ordered by price (cheapest first)
            String selectSql = "SELECT id, price FROM Ticket " +
                    "WHERE match_id = ? AND sold = 0 " +
                    "ORDER BY price ASC " +
                    "LIMIT ?";

            List<TicketInfo> ticketsToSell = new ArrayList<>();
            double totalCost = 0.0;

            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setLong(1, ticketSale.getMatchId());
                selectStmt.setInt(2, ticketSale.getSeatsPurchased());

                ResultSet rs = selectStmt.executeQuery();
                while (rs.next()) {
                    TicketInfo ticket = new TicketInfo(
                            rs.getLong("id"),
                            rs.getDouble("price")
                    );
                    ticketsToSell.add(ticket);
                    totalCost += ticket.getPrice();
                }
            }


            // 3. Update the tickets
            String updateSql = "UPDATE Ticket SET sold = 1, user_id = ? WHERE id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                for (TicketInfo ticket : ticketsToSell) {
                    updateStmt.setLong(1, customer.getId());
                    updateStmt.setLong(2, ticket.getId());
                    updateStmt.addBatch();
                }
                updateStmt.executeBatch();
            }

            connection.commit();
            return totalCost;

        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                throw new RepositoryException("Failed to rollback transaction " + ex.getMessage());
            }
            throw new RepositoryException("Error selling tickets: " + e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                throw new RepositoryException(String.format("Error closing connection %s", e));
            }
        }
    }

    @Override
    public List<Ticket> findTicketsBoughtByUser(User user) throws RepositoryException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT id, match_id, seatNumber, sold, price, user_id FROM Ticket WHERE user_id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, user.getId());
            try(ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Long ticketId = rs.getLong("id");
                    Long matchId = rs.getLong("match_id");
                    int seatNumber = rs.getInt("seatNumber");
                    boolean sold = rs.getInt("sold") != 0;
                    double price = rs.getDouble("price");


                    // Create minimal Match object
                    Match match = new Match();
                    match.setId(matchId);

                    tickets.add(new Ticket(ticketId, match, seatNumber, sold, price, Optional.of(user)));
                }
            }
        } catch (SQLException e) {
            log.error("Error retrieving all tickets", e);
        }
        return tickets;
    }

    // Helper class to store ticket info during transaction
    private static class TicketInfo {
        private final Long id;
        private final Double price;

        public TicketInfo(Long id, Double price) {
            this.id = id;
            this.price = price;
        }

        public Long getId() { return id; }
        public Double getPrice() { return price; }
    }
}