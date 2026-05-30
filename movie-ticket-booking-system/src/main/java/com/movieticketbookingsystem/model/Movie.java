package com.movieticketbookingsystem.model;

import com.movieticketbookingsystem.observer.MovieSubject;

public class Movie extends MovieSubject {
    private final String id;
    private final String title;
    private final String genre;
    private final String language;
    private final int durationInMinutes;

    public Movie(String id, String title, String genre, String language, int durationInMinutes) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.language = language;
        this.durationInMinutes = durationInMinutes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public String getLanguage() {
        return language;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }
}
