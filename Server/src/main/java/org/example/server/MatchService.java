package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.UserType;
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
}