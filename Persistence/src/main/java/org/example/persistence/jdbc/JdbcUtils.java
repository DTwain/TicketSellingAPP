package org.example.persistence.jdbc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcUtils {
    private Properties props;
    private static final Logger logger = LogManager.getLogger(JdbcUtils.class);
    private static Connection instance = null;

    public JdbcUtils(Properties props) {
        this.props = props;
    }

    private Connection getNewConnection() {
        String url = props.getProperty("jdbc.url");
        String user = props.getProperty("jdbc.user");
        String pass = props.getProperty("jdbc.pass");
        logger.info("Trying to connect to database... {}", url);
        logger.info("User: {}", user);
        logger.info("Pass: {}", pass);

        Connection con = null;
        try {
            if (user != null && pass != null)
                con = DriverManager.getConnection(url, user, pass);
            else
                con = DriverManager.getConnection(url);
        } catch (SQLException e) {
            logger.error("Error getting connection", e);
            System.out.println("Error getting connection: " + e);
        }
        return con;
    }

    public Connection getConnection() {
        logger.traceEntry();
        try {
            if (instance == null || instance.isClosed())
                instance = getNewConnection();
        } catch (SQLException e) {
            logger.error("Error DB", e);
            System.out.println("Error DB: " + e);
        }
        logger.traceExit(instance);
        return instance;
    }

    public void closeConnection() {
        if (instance != null) {
            try {
                instance.close();
            } catch (SQLException e) {
                logger.error(e);
            }
        }
    }
}