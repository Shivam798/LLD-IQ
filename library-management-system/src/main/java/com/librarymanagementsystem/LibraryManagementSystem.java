package com.librarymanagementsystem;

import com.librarymanagementsystem.enums.ItemType;
import com.librarymanagementsystem.factory.ItemFactory;
import com.librarymanagementsystem.model.BookCopy;
import com.librarymanagementsystem.model.LibraryItem;
import com.librarymanagementsystem.model.Member;
import com.librarymanagementsystem.strategy.SearchStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LibraryManagementSystem {
    private static final LibraryManagementSystem INSTANCE = new LibraryManagementSystem();

    private final Map<String, LibraryItem> catalog = new ConcurrentHashMap<>();
    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private final Map<String, BookCopy> copies = new ConcurrentHashMap<>();

    private LibraryManagementSystem() {}

    public static LibraryManagementSystem getInstance() {
        return INSTANCE;
    }

    public List<BookCopy> addItem(ItemType type, String id, String title, String authorOrPublisher,
                                  String isbn, int publicationYear, int numCopies) {
        LibraryItem item = ItemFactory.createItem(type, id, title, authorOrPublisher, isbn, publicationYear);
        catalog.put(id, item);
        List<BookCopy> bookCopies = new ArrayList<>();
        for (int i = 1; i <= numCopies; i++) {
            String copyId = id + "-c" + i;
            BookCopy copy = new BookCopy(copyId, item);
            copies.put(copyId, copy);
            bookCopies.add(copy);
        }
        System.out.println("Added " + numCopies + " copies of '" + title + "' (id=" + id + ")");
        return bookCopies;
    }

    public boolean removeItem(String itemId) {
        LibraryItem item = catalog.get(itemId);
        if (item == null) {
            return false;
        }
        boolean anyCheckedOut = item.getCopies().stream().anyMatch(c -> !c.isAvailable());
        if (anyCheckedOut) {
            System.out.println("Cannot remove '" + item.getTitle() + "' — copies are still checked out.");
            return false;
        }
        item.getCopies().forEach(c -> copies.remove(c.getId()));
        catalog.remove(itemId);
        System.out.println("Removed '" + item.getTitle() + "' from catalog.");
        return true;
    }

    public Member addMember(String id, String name, String contactInfo) {
        Member member = new Member(id, name, contactInfo);
        members.put(id, member);
        System.out.println("Registered member " + name + " (id=" + id + ")");
        return member;
    }

    public void checkout(String memberId, String copyId) {
        Member member = members.get(memberId);
        BookCopy copy = copies.get(copyId);
        if (member == null || copy == null) {
            System.out.println("Error: Invalid member or copy ID.");
            return;
        }
        copy.checkout(member);
    }

    public void returnItem(String copyId) {
        BookCopy copy = copies.get(copyId);
        if (copy == null) {
            System.out.println("Error: Invalid copy ID.");
            return;
        }
        copy.returnItem();
    }

    public void placeHold(String memberId, String itemId) {
        Member member = members.get(memberId);
        LibraryItem item = catalog.get(itemId);
        if (member == null || item == null) {
            System.out.println("Error: Invalid member or item ID.");
            return;
        }
        Optional<BookCopy> checkedOut = item.getCopies().stream()
                .filter(c -> !c.isAvailable())
                .findFirst();
        if (checkedOut.isPresent()) {
            checkedOut.get().placeHold(member);
        } else {
            System.out.println("  All copies of '" + item.getTitle() + "' are available — checkout instead.");
        }
    }

    public List<LibraryItem> search(String query, SearchStrategy strategy) {
        return strategy.search(query, new ArrayList<>(catalog.values()));
    }

    public void printCatalog() {
        System.out.println("\n--- Library Catalog ---");
        catalog.values().forEach(item -> System.out.printf(
                "  %s | %-25s | by %-20s | available: %d/%d%n",
                item.getId(), item.getTitle(), item.getAuthorOrPublisher(),
                item.getAvailableCopyCount(), item.getCopies().size()));
        System.out.println("-----------------------\n");
    }

    public Optional<LibraryItem> getItem(String itemId) {
        return Optional.ofNullable(catalog.get(itemId));
    }

    public Optional<Member> getMember(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }
}
