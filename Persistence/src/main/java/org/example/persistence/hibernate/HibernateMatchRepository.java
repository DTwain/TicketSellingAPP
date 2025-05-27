package org.example.persistence.hibernate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.persistence.RepositoryException;
import org.example.persistence.entities.MatchEntity;
import org.example.persistence.interfaces.MatchInterface;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.persistence.Query;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class HibernateMatchRepository implements MatchInterface {
    private final SessionFactory sessionFactory;
    private static final Logger logger = LogManager.getLogger(HibernateMatchRepository.class);

    public HibernateMatchRepository(Properties properties) {
        this.sessionFactory = HibernateUtil.getSessionFactory(properties);
        logger.info("HibernateMatchRepository initialized");
    }

    @Override
    public Optional<Match> findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }

        try (Session session = sessionFactory.openSession()) {
            MatchEntity entity = session.get(MatchEntity.class, id);
            if (entity != null) {
                return Optional.of(entity.toDomain());
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error finding Match with id {}", id, e);
            throw new RepositoryException("Error finding match", (SQLException) e);
        }
    }

    @Override
    public Iterable<Match> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query query = session.createQuery("FROM MatchEntity", MatchEntity.class);
            List<MatchEntity> entities = query.getResultList();

            List<Match> matches = new ArrayList<>();
            for (MatchEntity entity : entities) {
                matches.add(entity.toDomain());
            }
            return matches;
        } catch (Exception e) {
            logger.error("Error retrieving all matches", e);
            throw new RepositoryException("Error retrieving matches", (SQLException) e);
        }
    }

    @Override
    public Optional<Match> save(Match entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }

        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            MatchEntity matchEntity = MatchEntity.fromDomain(entity);
            session.save(matchEntity);

            transaction.commit();

            // Update the domain object with the generated ID
            entity.setId(matchEntity.getId());
            logger.info("Saved match with ID: {}", matchEntity.getId());
            return Optional.empty(); // Success
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error saving Match: {}", entity, e);
            throw new RepositoryException("Error saving match", (SQLException) e);
        }
    }

    @Override
    public Optional<Match> delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }

        Optional<Match> matchOpt = findOne(id);
        if (matchOpt.isEmpty()) {
            return Optional.empty();
        }

        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            MatchEntity entity = session.get(MatchEntity.class, id);
            if (entity != null) {
                session.delete(entity);
                transaction.commit();
                logger.info("Deleted match with ID: {}", id);
                return matchOpt;
            }

            transaction.commit();
            return Optional.empty();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error deleting Match with id {}", id, e);
            throw new RepositoryException("Error deleting match", (SQLException) e);
        }
    }

    @Override
    public Optional<Match> update(Match entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }

        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            MatchEntity matchEntity = MatchEntity.fromDomain(entity);
            session.update(matchEntity);

            transaction.commit();
            logger.info("Updated match with ID: {}", entity.getId());
            return Optional.empty(); // Success
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error updating Match: {}", entity, e);
            return Optional.of(entity); // Return entity on failure
        }
    }
}