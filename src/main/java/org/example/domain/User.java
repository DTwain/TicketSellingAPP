package org.example.domain;

import java.time.LocalDateTime;

public class User extends Entity<Long> {
    private String username;
    private String password;

    public User() {
    }

    public User(Long id, String username, String password) {
        setId(id);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}