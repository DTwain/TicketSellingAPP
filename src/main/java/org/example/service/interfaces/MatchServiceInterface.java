package org.example.service.interfaces;

import org.example.domain.Match;
import org.example.service.ServicesException;
import org.example.utils.events.EntityChangeType;
import org.example.utils.observer.Observable;
import org.example.utils.observer.Observer;

import java.util.List;
import java.util.Optional;

public interface MatchServiceInterface extends Observable<EntityChangeType<?>> {
    List<Match> findAll() throws ServicesException;
    public Optional<Match> findOne(Long id) throws ServicesException;
}
