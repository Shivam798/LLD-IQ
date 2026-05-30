package com.movieticketbookingsystem.observer;

import com.movieticketbookingsystem.model.Movie;
import com.movieticketbookingsystem.model.User;

public class UserObserver implements MovieObserver {
    private final User user;

    public UserObserver(User user) {
        this.user = user;
    }

    @Override
    public void update(Movie movie) {
        System.out.printf("[Notification] %s (%s): '%s' is now open for booking!%n",
                user.getName(), user.getEmail(), movie.getTitle());
    }
}
