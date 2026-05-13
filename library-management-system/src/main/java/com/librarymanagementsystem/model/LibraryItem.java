package com.librarymanagementsystem.model;

import com.librarymanagementsystem.observer.HoldObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class LibraryItem {
    private final String id;
    private final String title;
    private final String isbn;
    private final int publicationYear;
    private final List<BookCopy> copies = new CopyOnWriteArrayList<>();
    private final List<HoldObserver> holdQueue = new CopyOnWriteArrayList<>();

    protected LibraryItem(String id, String title, String isbn, int publicationYear) {
        this.id = id;
        this.title = title;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
    }

    public void addCopy(BookCopy copy) {
        copies.add(copy);
    }

    public void addObserver(HoldObserver observer) {
        if (!holdQueue.contains(observer)) {
            holdQueue.add(observer);
        }
    }

    public void removeObserver(HoldObserver observer) {
        holdQueue.remove(observer);
    }

    public boolean hasObservers() {
        return !holdQueue.isEmpty();
    }

    public void notifyObservers() {
        for (HoldObserver observer : holdQueue) {
            observer.update(this);
        }
    }

    public boolean isObserver(HoldObserver observer) {
        return holdQueue.contains(observer);
    }

    public long getAvailableCopyCount() {
        return copies.stream().filter(BookCopy::isAvailable).count();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public List<BookCopy> getCopies() {
        return copies;
    }

    public abstract String getAuthorOrPublisher();
}
