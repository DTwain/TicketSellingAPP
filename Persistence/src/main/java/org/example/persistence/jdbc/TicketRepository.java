package org.example.persistence.jdbc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.persistence.RepositoryException;
import org.example.persistence.interfaces.MatchInterface;
import org.example.persistence.interfaces.TicketInterface;

import java.sql.*;
import java.util.*;

public class TicketRepository implements TicketInterface {
    private JdbcUtils jdbcUtils;
    private MatchInterface matchRepository;
    private static final Logger logger = LogManager.getLogger(TicketRepository.class);

    public TicketRepository(Properties properties, MatchInterface matchRepository) {
        logger.info("Initializing TicketRepository...");
        this.jdbcUtils = new JdbcUtils(properties);
        this.matchRepository = matchRepository;
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

                    // Get the match
                    Optional<Match> matchOpt = matchRepository.findOne(matchId);
                    if (matchOpt.isEmpty()) {
                        logger.error("Match not found for ticket with id {}", id);
                        return Optional.empty();
                    }
                    Match match = matchOpt.get();

                    // Create a ticket with empty user for now
                    Optional<User> user = Optional.empty();
                    if (userId != null) {
                        // In a real implementation, you would fetch the user from a user repository
                        user = Optional.of(new User(userId, "", ""));
                    }

                    return Optional.of(new Ticket(ticketId, match, seatNumber, sold, price, user));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding Ticket with id {}", id, e);
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

                // Get the match
                Optional<Match> matchOpt = matchRepository.findOne(matchId);
                if (matchOpt.isEmpty()) {
                    logger.error("Match not found for ticket with id {}", ticketId);
                    continue;
                }
                Match match = matchOpt.get();

                // Create a ticket with empty user for now
                Optional<User> user = Optional.empty();
                if (userId != null) {
                    // In a real implementation, you would fetch the user from a user repository
                    user = Optional.of(new User(userId, "", ""));
                }

                tickets.add(new Ticket(ticketId, match, seatNumber, sold, price, user));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all tickets", e);
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
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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
            if (rows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setId(generatedKeys.getLong(1));
                    }
                }
                logger.info("Inserted {} row(s) into Ticket table for ticket id={}.", rows, entity.getId());
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.error("Error saving Ticket: {}", entity, e);
        }
        return Optional.of(entity);
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
            logger.info("Deleted {} row(s) from Ticket table for ticket id={}.", rows, id);
            if (rows > 0) {
                return ticketOpt;
            }
        } catch (SQLException e) {
            logger.error("Error deleting Ticket with id {}", id, e);
        }
        return Optional.empty();
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
                logger.warn("No Ticket record found with id {} for update.", entity.getId());
                return Optional.of(entity);
            }
            logger.info("Updated {} row(s) in Ticket table for ticket id={}.", rows, entity.getId());
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error updating Ticket: {}", entity, e);
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
            throw new RepositoryException(String.format("Error counting available tickets: %s", e));
        }
    }

    @Override
    public String ticketPriceRangeByMatch(Long matchId) throws RepositoryException {
        String sql = "SELECT MIN(price), MAX(price) FROM Ticket WHERE match_id = ? AND sold = 0";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, matchId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double minPrice = rs.getDouble(1);
                double maxPrice = rs.getDouble(2);
                return String.format("$%.2f - $%.2f", minPrice, maxPrice);
            }
            return "N/A";
        } catch (SQLException e) {
            throw new RepositoryException(String.format("Error getting price range: %s", e));
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

            if (ticketsToSell.size() < ticketSale.getSeatsPurchased()) {
                throw new RepositoryException("Not enough tickets available");
            }

            // 2. Update the tickets
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
                throw new RepositoryException("Failed to rollback transaction: " + ex.getMessage());
            }
            throw new RepositoryException("Error selling tickets: " + e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new RepositoryException("Error resetting auto-commit: " + e.getMessage());
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
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Long ticketId = rs.getLong("id");
                    Long matchId = rs.getLong("match_id");
                    int seatNumber = rs.getInt("seatNumber");
                    boolean sold = rs.getInt("sold") != 0;
                    double price = rs.getDouble("price");

                    // Get the match
                    Optional<Match> matchOpt = matchRepository.findOne(matchId);
                    if (matchOpt.isEmpty()) {
                        logger.error("Match not found for ticket with id {}", ticketId);
                        continue;
                    }
                    Match match = matchOpt.get();

                    tickets.add(new Ticket(ticketId, match, seatNumber, sold, price, Optional.of(user)));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding tickets for user: " + e.getMessage());
        }
        return tickets;
    }

    @Override
    public List<Ticket> findTicketsByCustomer(String name) throws RepositoryException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.id, t.match_id, t.seatNumber, t.sold, t.price, t.user_id " +
                "FROM Ticket t JOIN User u ON t.user_id = u.id " +
                "WHERE u.username = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Long ticketId = rs.getLong("id");
                    Long matchId = rs.getLong("match_id");
                    int seatNumber = rs.getInt("seatNumber");
                    boolean sold = rs.getInt("sold") != 0;
                    double price = rs.getDouble("price");
                    Long userId = rs.getLong("user_id");

                    // Get the match
                    Optional<Match> matchOpt = matchRepository.findOne(matchId);
                    if (matchOpt.isEmpty()) {
                        logger.error("Match not found for ticket with id {}", ticketId);
                        continue;
                    }
                    Match match = matchOpt.get();

                    // Create a simple user object with just the ID and name
                    User user = new User(userId, name, "");
                    tickets.add(new Ticket(ticketId, match, seatNumber, sold, price, Optional.of(user)));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding tickets for customer: " + e.getMessage());
        }
        return tickets;
    }

    // Helper class for ticket sales transaction
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