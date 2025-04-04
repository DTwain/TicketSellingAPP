package org.example.repository.interfaces;

import org.example.domain.User;
import org.example.repository.Repository;

import java.util.Optional;

public interface UserInterface extends Repository<Long, User> {
    public Optional<User> findUserByUsername(String username);
}
