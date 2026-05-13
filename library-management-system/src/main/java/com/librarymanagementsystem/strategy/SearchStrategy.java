package com.librarymanagementsystem.strategy;

import com.librarymanagementsystem.model.LibraryItem;

import java.util.List;

public interface SearchStrategy {
    List<LibraryItem> search(String query, List<LibraryItem> items);
}
