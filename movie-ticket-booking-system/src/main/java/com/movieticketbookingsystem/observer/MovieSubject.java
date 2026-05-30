package com.movieticketbookingsystem.observer;

import com.movieticketbookingsystem.model.Movie;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class MovieSubject {
    private final List<MovieObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(MovieObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(MovieObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (MovieObserver observer : observers) {
            observer.update((Movie) this);
        }
    }
}
