package org.example.repository;

import org.example.domain.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class UserRepository implements Repository<Long, User> {
    private static final Logger log = LogManager.getLogger(UserRepository.class);
    private String url;

    public UserRepository() {
        log.info("Initializing UserDbRepository...");
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
    public Optional<User> findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }
        String sql = "SELECT id, username, password FROM User WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long userId = rs.getLong("id");
                    String username = rs.getString("username");
                    String password = rs.getString("password");
                    User user = new User(userId, username, password);
                    return Optional.of(user);
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
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Long id = rs.getLong("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                User user = new User(id, username, password);
                users.add(user);
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
        String sql = "INSERT INTO User (id, username, password) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, entity.getId());
            stmt.setString(2, entity.getUsername());
            stmt.setString(3, entity.getPassword());

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
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.getUsername());
            stmt.setString(2, entity.getPassword());
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
}
