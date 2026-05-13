package com.librarymanagementsystem.model;

public class Book extends LibraryItem {
    private final String author;

    public Book(String id, String title, String author, String isbn, int publicationYear) {
        super(id, title, isbn, publicationYear);
        this.author = author;
    }

    @Override
    public String getAuthorOrPublisher() {
        return author;
    }
}
