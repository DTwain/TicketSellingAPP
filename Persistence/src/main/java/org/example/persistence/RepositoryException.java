package org.example.persistence;

import java.sql.SQLException;

public class RepositoryException extends RuntimeException {
    public RepositoryException(){}

    public RepositoryException(String message){
        super(message);
    }
    public RepositoryException(Exception ex){
        super(ex);
    }

    public RepositoryException(String s, SQLException e) {
    }
}