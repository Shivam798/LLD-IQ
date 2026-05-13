package com.librarymanagementsystem.strategy;

import com.librarymanagementsystem.model.LibraryItem;

import java.util.List;
import java.util.stream.Collectors;

public class SearchByTitleStrategy implements SearchStrategy {
    @Override
    public List<LibraryItem> search(String query, List<LibraryItem> items) {
        String needle = query.toLowerCase();
        return items.stream()
                .filter(item -> item.getTitle().toLowerCase().contains(needle))
                .collect(Collectors.toList());
    }
}
