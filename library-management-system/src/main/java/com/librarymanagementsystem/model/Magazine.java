package com.librarymanagementsystem.model;

public class Magazine extends LibraryItem {
    private final String publisher;

    public Magazine(String id, String title, String publisher, String isbn, int publicationYear) {
        super(id, title, isbn, publicationYear);
        this.publisher = publisher;
    }

    @Override
    public String getAuthorOrPublisher() {
        return publisher;
    }
}
