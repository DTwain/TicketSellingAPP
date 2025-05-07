package org.example.persistence.interfaces;

import org.example.domain.User;
import org.example.persistence.Repository;

import java.util.Optional;

public interface UserInterface extends Repository<Long, User> {
    Optional<User> findUserByUsername(String username);
}