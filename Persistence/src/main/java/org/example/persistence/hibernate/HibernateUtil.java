package org.example.persistence.hibernate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.example.persistence.entities.MatchEntity;
import org.example.persistence.entities.TicketEntity;

import java.util.Properties;

public class HibernateUtil {
    private static SessionFactory sessionFactory;
    private static final Logger logger = LogManager.getLogger(HibernateUtil.class);

    public static SessionFactory getSessionFactory(Properties dbProperties) {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();

                // Database connection properties
                Properties hibernateProperties = new Properties();
                hibernateProperties.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
                hibernateProperties.setProperty("hibernate.connection.url", dbProperties.getProperty("jdbc.url"));
                // Use the correct package path for our custom dialect
                hibernateProperties.setProperty("hibernate.dialect", "org.example.persistence.hibernate.SQLiteDialect");
                hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "none"); // Don't auto-create tables
                hibernateProperties.setProperty("hibernate.show_sql", "true");
                hibernateProperties.setProperty("hibernate.format_sql", "true");

                // Connection pool settings
                hibernateProperties.setProperty("hibernate.connection.pool_size", "10");
                hibernateProperties.setProperty("hibernate.current_session_context_class", "thread");

                configuration.setProperties(hibernateProperties);

                // Add annotated entity classes
                configuration.addAnnotatedClass(MatchEntity.class);
                configuration.addAnnotatedClass(TicketEntity.class);

                ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties())
                        .build();

                sessionFactory = configuration.buildSessionFactory(serviceRegistry);
                logger.info("Hibernate SessionFactory created successfully");

            } catch (Exception e) {
                logger.error("Error creating Hibernate SessionFactory", e);
                throw new RuntimeException("Failed to create SessionFactory", e);
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}