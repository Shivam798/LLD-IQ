package com.librarymanagementsystem.observer;

import com.librarymanagementsystem.model.LibraryItem;

public interface HoldObserver {
    void update(LibraryItem item);
}
