package com.librarymanagementsystem.strategy;

import com.librarymanagementsystem.model.LibraryItem;

import java.util.List;
import java.util.stream.Collectors;

public class SearchByIsbnStrategy implements SearchStrategy {
    @Override
    public List<LibraryItem> search(String query, List<LibraryItem> items) {
        return items.stream()
                .filter(item -> item.getIsbn().equalsIgnoreCase(query))
                .collect(Collectors.toList());
    }
}
