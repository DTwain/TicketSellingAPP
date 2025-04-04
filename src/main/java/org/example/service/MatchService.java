package org.example.service;

import org.example.domain.Match;
import org.example.repository.RepositoryException;
import org.example.repository.interfaces.MatchInterface;
import org.example.service.interfaces.MatchServiceInterface;
import org.example.utils.events.EntityChangeType;
import org.example.utils.observer.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MatchService implements MatchServiceInterface {
    private List<Observer<EntityChangeType<?>>> observers;
    private MatchInterface matchInterface;
    public MatchService(MatchInterface matchInterface) {
        this.matchInterface = matchInterface;
        observers = new ArrayList<>();
    }

    @Override
    public List<Match> findAll() throws ServicesException {
        try {
            return (List<Match>) matchInterface.findAll();
        } catch (RepositoryException e) {
            throw new ServicesException(e);
        }
    }

    @Override
    public Optional<Match> findOne(Long id) throws ServicesException {
        try {
            return (Optional<Match>) matchInterface.findOne(id);
        } catch (RepositoryException e) {
            throw new ServicesException(e);
        }
    }

    @Override
    public void addObserver(Observer<EntityChangeType<?>> e) {
        observers.add(e);
    }

    @Override
    public void removeObserver(Observer<EntityChangeType<?>> e) {
        observers.remove(e);
    }

    @Override
    public void notifyObservers(EntityChangeType<?> t) {
        for (Observer<EntityChangeType<?>> observer : observers) {
            observer.update(t);
        }
    }
}
