package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.persistence.RepositoryException;
import org.example.persistence.interfaces.MatchInterface;
import org.example.service.ServicesException;
import org.example.service.interfaces.MatchServiceInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MatchService implements MatchServiceInterface {
    private final MatchInterface matchRepository;
    private static final Logger logger = LogManager.getLogger(MatchService.class);

    public MatchService(MatchInterface matchRepository) {
        this.matchRepository = matchRepository;
        logger.info("MatchService initialized");
    }

    @Override
    public List<Match> findAll() throws ServicesException {
        try {
            List<Match> matches = new ArrayList<>();
            matchRepository.findAll().forEach(matches::add);
            return matches;
        } catch (RepositoryException e) {
            logger.error("Error finding all matches", e);
            throw new ServicesException("Error retrieving matches", e);
        }
    }

    @Override
    public Optional<Match> findOne(Long id) throws ServicesException {
        try {
            return matchRepository.findOne(id);
        } catch (RepositoryException e) {
            logger.error("Error finding match with id {}", id, e);
            throw new ServicesException("Error retrieving match", e);
        }
    }

    @Override
    public Match save(Match match) throws ServicesException {
        try {
            Optional<Match> result = matchRepository.save(match);
            if (result.isEmpty()) {
                // Success - entity was saved
                return match;
            } else {
                // Error - entity was returned indicating save failed
                throw new ServicesException("Failed to save match");
            }
        } catch (RepositoryException e) {
            logger.error("Error saving match", e);
            throw new ServicesException("Error saving match", e);
        }
    }

    @Override
    public Match update(Match match) throws ServicesException {
        try {
            Optional<Match> result = matchRepository.update(match);
            if (result.isEmpty()) {
                // Success - entity was updated
                return match;
            } else {
                // Error - entity was returned indicating update failed
                throw new ServicesException("Failed to update match");
            }
        } catch (RepositoryException e) {
            logger.error("Error updating match", e);
            throw new ServicesException("Error updating match", e);
        }
    }

    @Override
    public void delete(Long id) throws ServicesException {
        try {
            Optional<Match> result = matchRepository.delete(id);
            if (result.isEmpty()) {
                throw new ServicesException("Match not found for deletion");
            }
        } catch (RepositoryException e) {
            logger.error("Error deleting match with id {}", id, e);
            throw new ServicesException("Error deleting match", e);
        }
    }
}