package com.librarymanagementsystem.factory;

import com.librarymanagementsystem.enums.ItemType;
import com.librarymanagementsystem.model.Book;
import com.librarymanagementsystem.model.LibraryItem;
import com.librarymanagementsystem.model.Magazine;

public class ItemFactory {
    private ItemFactory() {}

    public static LibraryItem createItem(ItemType type, String id, String title,
                                         String authorOrPublisher, String isbn, int publicationYear) {
        return switch (type) {
            case BOOK -> new Book(id, title, authorOrPublisher, isbn, publicationYear);
            case MAGAZINE -> new Magazine(id, title, authorOrPublisher, isbn, publicationYear);
        };
    }
}
