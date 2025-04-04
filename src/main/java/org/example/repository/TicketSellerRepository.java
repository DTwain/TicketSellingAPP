package org.example.repository;

import org.example.domain.TicketSeller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.repository.interfaces.TicketSellerRepoInterface;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class TicketSellerRepository implements TicketSellerRepoInterface {
    private JdbcUtils jdbcUtils;
    private static final Logger log = LogManager.getLogger(TicketSellerRepository.class);

    public TicketSellerRepository(Properties properties) {
        this.jdbcUtils = new JdbcUtils(properties);
    }

    @Override
    public Optional<TicketSeller> findOne(Long id) {
        String sql = "SELECT id, username FROM TicketSeller WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long sellerId = rs.getLong("id");
                    String username = rs.getString("username");
                    return Optional.of(new TicketSeller(sellerId, username));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding ticket seller by id {}", id, e);
        }
        return Optional.empty();
    }

    @Override
        public Optional<TicketSeller> findByUsername(String username) {
        String sql = "SELECT id, username FROM TicketSeller WHERE username = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong("id");
                    return Optional.of(new TicketSeller(id, username));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding ticket seller by username {}", username, e);
        }
        return Optional.empty();
    }

    @Override
    public Iterable<TicketSeller> findAll() {
        List<TicketSeller> sellers = new ArrayList<>();
        String sql = "SELECT id, username FROM TicketSeller";

        try (Connection connection = jdbcUtils.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                String username = rs.getString("username");
                sellers.add(new TicketSeller(id, username));
            }
        } catch (SQLException e) {
            log.error("Error retrieving all ticket sellers", e);
        }
        return sellers;
    }

    @Override
    public Optional<TicketSeller> save(TicketSeller entity) {
        String sql = "INSERT INTO TicketSeller (username) VALUES (?)";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, entity.getUsername());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return Optional.empty();
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setId(generatedKeys.getLong(1));
                    return Optional.of(entity);
                }
            }
        } catch (SQLException e) {
            log.error("Error saving ticket seller: {}", entity, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<TicketSeller> delete(Long id) {
        Optional<TicketSeller> sellerOpt = findOne(id);
        if (sellerOpt.isEmpty()) {
            return Optional.empty();
        }

        String sql = "DELETE FROM TicketSeller WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                return sellerOpt;
            }
        } catch (SQLException e) {
            log.error("Error deleting ticket seller with id {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<TicketSeller> update(TicketSeller entity) {
        String sql = "UPDATE TicketSeller SET username = ? WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, entity.getUsername());
            stmt.setLong(2, entity.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                return Optional.of(entity);
            }
        } catch (SQLException e) {
            log.error("Error updating ticket seller: {}", entity, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean isTicketSeller(String username) {
        return findByUsername(username).isPresent();
    }
}