package org.example.persistence.jdbc;

import org.example.domain.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.persistence.interfaces.UserInterface;
import org.example.persistence.crypto.CryptoUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;


public class UserRepository implements UserInterface {
    private JdbcUtils jdbcUtils;
    private static final Logger log = LogManager.getLogger(UserRepository.class);

    public UserRepository(Properties properties) {
        log.info("Initializing UserDbRepository...");
        jdbcUtils = new JdbcUtils(properties);
    }

    @Override
    public Optional<User> findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }

        String sql = "SELECT id, username, password FROM User WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long userId = rs.getLong("id");
                    String username = rs.getString("username");
                    String password = rs.getString("password");
                    String passwordDecrypted = CryptoUtil.decrypt(password);
                    return Optional.of(new User(userId, username, passwordDecrypted));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding User with id {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public Iterable<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password FROM User";

        try (Connection connection = jdbcUtils.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String passwordDecrypted = CryptoUtil.decrypt(password);
                users.add(new User(id, username, passwordDecrypted));
            }
        } catch (SQLException e) {
            log.error("Error retrieving all users", e);
        }
        return users;
    }

    @Override
    public Optional<User> save(User entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }

        String sql = "INSERT INTO User (username, password) VALUES (?, ?)";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, entity.getUsername());

            String password = entity.getPassword();
            String encryptedPassword = CryptoUtil.encrypt(password);
            stmt.setString(2, encryptedPassword);

            int rows = stmt.executeUpdate();
            log.info("Inserted {} row(s) into User table for user id={}.", rows, entity.getId());
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Error saving User: {}", entity, e);
            return Optional.of(entity);
        }
    }

    @Override
    public Optional<User> delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }

        Optional<User> userOpt = findOne(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        String sql = "DELETE FROM User WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            log.info("Deleted {} row(s) from User table for user id={}.", rows, id);
        } catch (SQLException e) {
            log.error("Error deleting User with id {}", id, e);
        }
        return userOpt;
    }

    @Override
    public Optional<User> update(User entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }

        String sql = "UPDATE User SET username = ?, password = ? WHERE id = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, entity.getUsername());
            String encryptedPassword = CryptoUtil.encrypt(entity.getPassword());
            stmt.setString(2, encryptedPassword);
            stmt.setLong(3, entity.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                log.warn("No User record found with id {} for update.", entity.getId());
                return Optional.of(entity);
            }
            log.info("Updated {} row(s) in User table for user id={}.", rows, entity.getId());
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Error updating User: {}", entity, e);
            return Optional.of(entity);
        }
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }

        String sql = "SELECT id, username, password FROM User WHERE username = ?";

        try (Connection connection = jdbcUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long userId = rs.getLong("id");
                    String passwordEntrypted = rs.getString("password");
                    String passwordDecrypted = CryptoUtil.decrypt(passwordEntrypted);
                    return Optional.of(new User(userId, username, passwordDecrypted));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding User with username {}", username, e);
        }
        return Optional.empty();
    }
}