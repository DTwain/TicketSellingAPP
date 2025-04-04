package org.example.service.interfaces;

import org.example.domain.User;
import org.example.service.ServicesException;

import java.util.Optional;

public interface UserServiceInterface {
    boolean authenticate(String username, String password) throws ServicesException;
    boolean usernameExists(String username) throws ServicesException;
    boolean registerUser(String username, String password) throws ServicesException;
    Optional<User> getUserByUsername(String username) throws ServicesException;
}