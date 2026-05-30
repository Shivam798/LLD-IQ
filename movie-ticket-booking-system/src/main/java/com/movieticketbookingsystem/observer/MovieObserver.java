package com.movieticketbookingsystem.observer;

import com.movieticketbookingsystem.model.Movie;

public interface MovieObserver {
    void update(Movie movie);
}
