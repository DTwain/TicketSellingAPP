package org.example.service.interfaces;

import org.example.domain.Match;
import org.example.service.ServicesException;

import java.util.List;
import java.util.Optional;

public interface MatchServiceInterface {
    List<Match> findAll() throws ServicesException;
    Optional<Match> findOne(Long id) throws ServicesException;
    Match save(Match match) throws ServicesException;
    Match update(Match match) throws ServicesException;
    void delete(Long id) throws ServicesException;
}