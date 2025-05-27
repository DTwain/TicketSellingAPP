package org.example.persistence.hibernate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.persistence.RepositoryException;
import org.example.persistence.entities.TicketEntity;
import org.example.persistence.interfaces.MatchInterface;
import org.example.persistence.interfaces.TicketInterface;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.persistence.Query;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class HibernateTicketRepository implements TicketInterface {
    private final SessionFactory sessionFactory;
    private final MatchInterface matchRepository;
    private static final Logger logger = LogManager.getLogger(HibernateTicketRepository.class);

    public HibernateTicketRepository(Properties properties, MatchInterface matchRepository) {
        this.sessionFactory = HibernateUtil.getSessionFactory(properties);
        this.matchRepository = matchRepository;
        logger.info("HibernateTicketRepository initialized");
    }

    @Override
    public Optional<Ticket> findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }

        try (Session session = sessionFactory.openSession()) {
            TicketEntity entity = session.get(TicketEntity.class, id);
            if (entity != null) {
                return Optional.of(convertToTicket(entity));
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error finding Ticket with id {}", id, e);
            throw new RepositoryException("Error finding ticket", (SQLException) e);
        }
    }

    @Override
    public Iterable<Ticket> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query query = session.createQuery("FROM TicketEntity", TicketEntity.class);
            List<TicketEntity> entities = query.getResultList();

            List<Ticket> tickets = new ArrayList<>();
            for (TicketEntity entity : entities) {
                tickets.add(convertToTicket(entity));
            }
            return tickets;
        } catch (Exception e) {
            logger.error("Error retrieving all tickets", e);
            throw new RepositoryException("Error retrieving tickets", (SQLException) e);
        }
    }

    @Override
    public Optional<Ticket> save(Ticket entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }

        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            TicketEntity ticketEntity = TicketEntity.fromDomain(entity);
            session.save(ticketEntity);

            transaction.commit();

            entity.setId(ticketEntity.getId());
            logger.info("Saved ticket with ID: {}", ticketEntity.getId());
            return Optional.empty(); // Success
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error saving Ticket: {}", entity, e);
            throw new RepositoryException("Error saving ticket", (SQLException) e);
        }
    }

    @Override
    public Optional<Ticket> delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null.");
        }

        Optional<Ticket> ticketOpt = findOne(id);
        if (ticketOpt.isEmpty()) {
            return Optional.empty();
        }

        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            TicketEntity entity = session.get(TicketEntity.class, id);
            if (entity != null) {
                session.delete(entity);
                transaction.commit();
                logger.info("Deleted ticket with ID: {}", id);
                return ticketOpt;
            }

            transaction.commit();
            return Optional.empty();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error deleting Ticket with id {}", id, e);
            throw new RepositoryException("Error deleting ticket", (SQLException) e);
        }
    }

    @Override
    public Optional<Ticket> update(Ticket entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }

        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            TicketEntity ticketEntity = TicketEntity.fromDomain(entity);
            session.update(ticketEntity);

            transaction.commit();
            logger.info("Updated ticket with ID: {}", entity.getId());
            return Optional.empty(); // Success
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error updating Ticket: {}", entity, e);
            return Optional.of(entity); // Return entity on failure
        }
    }

    @Override
    public int countAvailableTicketsByMatch(Long matchId) throws RepositoryException {
        try (Session session = sessionFactory.openSession()) {
            Query query = session.createQuery(
                    "SELECT COUNT(*) FROM TicketEntity t WHERE t.matchId = :matchId AND t.sold = 0"
            );
            query.setParameter("matchId", matchId);
            Long count = (Long) query.getSingleResult();
            return count.intValue();
        } catch (Exception e) {
            logger.error("Error counting available tickets for match {}", matchId, e);
            throw new RepositoryException("Error counting available tickets", (SQLException) e);
        }
    }

    @Override
    public String ticketPriceRangeByMatch(Long matchId) throws RepositoryException {
        try (Session session = sessionFactory.openSession()) {
            Query query = session.createQuery(
                    "SELECT MIN(t.price), MAX(t.price) FROM TicketEntity t WHERE t.matchId = :matchId AND t.sold = 0"
            );
            query.setParameter("matchId", matchId);
            Object[] result = (Object[]) query.getSingleResult();

            if (result[0] != null && result[1] != null) {
                Double minPrice = (Double) result[0];
                Double maxPrice = (Double) result[1];
                return String.format("$%.2f - $%.2f", minPrice, maxPrice);
            }
            return "N/A";
        } catch (Exception e) {
            logger.error("Error getting price range for match {}", matchId, e);
            throw new RepositoryException("Error getting price range", (SQLException) e);
        }
    }

    @Override
    public Double sellTickets(TicketSale ticketSale, User customer) throws RepositoryException {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            // Get available tickets ordered by price
            Query selectQuery = session.createQuery(
                    "FROM TicketEntity t WHERE t.matchId = :matchId AND t.sold = 0 ORDER BY t.price ASC"
            );
            selectQuery.setParameter("matchId", ticketSale.getMatchId());
            selectQuery.setMaxResults(ticketSale.getSeatsPurchased());

            List<TicketEntity> ticketsToSell = selectQuery.getResultList();

            if (ticketsToSell.size() < ticketSale.getSeatsPurchased()) {
                throw new RepositoryException("Not enough tickets available");
            }

            double totalCost = 0.0;
            for (TicketEntity ticket : ticketsToSell) {
                ticket.setSold(1);
                ticket.setUserId(customer.getId());
                session.update(ticket);
                totalCost += ticket.getPrice();
            }

            transaction.commit();
            return totalCost;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error selling tickets", e);
            throw new RepositoryException("Error selling tickets", (SQLException) e);
        }
    }

    @Override
    public List<Ticket> findTicketsBoughtByUser(User user) throws RepositoryException {
        try (Session session = sessionFactory.openSession()) {
            Query query = session.createQuery(
                    "FROM TicketEntity t WHERE t.userId = :userId"
            );
            query.setParameter("userId", user.getId());

            List<TicketEntity> entities = query.getResultList();
            List<Ticket> tickets = new ArrayList<>();

            for (TicketEntity entity : entities) {
                tickets.add(convertToTicket(entity, Optional.of(user)));
            }

            return tickets;
        } catch (Exception e) {
            logger.error("Error finding tickets for user {}", user.getUsername(), e);
            throw new RepositoryException("Error finding user tickets", (SQLException) e);
        }
    }

    @Override
    public List<Ticket> findTicketsByCustomer(String name) throws RepositoryException {
        try (Session session = sessionFactory.openSession()) {
            // Note: This requires a join with User table, which we'd need to implement
            // For now, we'll implement a simpler version that assumes the name is available
            Query query = session.createQuery(
                    "FROM TicketEntity t WHERE t.userId IS NOT NULL"
            );

            List<TicketEntity> entities = query.getResultList();
            List<Ticket> tickets = new ArrayList<>();

            for (TicketEntity entity : entities) {
                // Create a minimal user object with the customer name
                User user = new User(entity.getUserId(), name, null);
                tickets.add(convertToTicket(entity, Optional.of(user)));
            }

            return tickets;
        } catch (Exception e) {
            logger.error("Error finding tickets for customer {}", name, e);
            throw new RepositoryException("Error finding customer tickets", (SQLException) e);
        }
    }

    private Ticket convertToTicket(TicketEntity entity) {
        return convertToTicket(entity, Optional.empty());
    }

    private Ticket convertToTicket(TicketEntity entity, Optional<User> user) {
        try {
            Optional<Match> matchOpt = matchRepository.findOne(entity.getMatchId());
            Match match = matchOpt.orElse(createMinimalMatch(entity.getMatchId()));

            return entity.toDomain(match, user);
        } catch (Exception e) {
            logger.error("Error converting ticket entity to domain", e);
            // Create minimal match if lookup fails
            Match minimalMatch = createMinimalMatch(entity.getMatchId());
            return entity.toDomain(minimalMatch, user);
        }
    }

    private Match createMinimalMatch(Long matchId) {
        Match match = new Match();
        match.setId(matchId);
        return match;
    }
}