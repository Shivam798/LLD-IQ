package com.librarymanagementsystem;

import com.librarymanagementsystem.enums.ItemType;
import com.librarymanagementsystem.model.BookCopy;
import com.librarymanagementsystem.model.Member;
import com.librarymanagementsystem.strategy.SearchByAuthorStrategy;
import com.librarymanagementsystem.strategy.SearchByTitleStrategy;

import java.util.List;

public class LibraryManagementDemo {
    public static void main(String[] args) {
        LibraryManagementSystem library = LibraryManagementSystem.getInstance();

        System.out.println("=== Setting up the Library ===");
        List<BookCopy> hobbitCopies = library.addItem(
                ItemType.BOOK, "B001", "The Hobbit", "J.R.R. Tolkien", "978-0345339683", 1937, 2);
        List<BookCopy> duneCopies = library.addItem(
                ItemType.BOOK, "B002", "Dune", "Frank Herbert", "978-0441172719", 1965, 1);
        library.addItem(
                ItemType.MAGAZINE, "M001", "National Geographic", "NatGeo Society", "0027-9358", 2024, 3);

        Member alice = library.addMember("MEM01", "Alice", "alice@mail.com");
        Member bob = library.addMember("MEM02", "Bob", "bob@mail.com");
        Member charlie = library.addMember("MEM03", "Charlie", "charlie@mail.com");
        library.printCatalog();

        System.out.println("=== Scenario 1: Searching (Strategy Pattern) ===");
        System.out.println("Search by title 'Dune':");
        library.search("Dune", new SearchByTitleStrategy())
                .forEach(item -> System.out.println("  found: " + item.getTitle()));
        System.out.println("Search by author 'Tolkien':");
        library.search("Tolkien", new SearchByAuthorStrategy())
                .forEach(item -> System.out.println("  found: " + item.getTitle()));

        System.out.println("\n=== Scenario 2: Checkout and Return (State Pattern) ===");
        library.checkout(alice.getId(), hobbitCopies.get(0).getId());
        library.checkout(bob.getId(), duneCopies.get(0).getId());
        library.printCatalog();

        System.out.println("Charlie attempts to checkout an already checked-out copy:");
        library.checkout(charlie.getId(), hobbitCopies.get(0).getId());

        System.out.println("\nAlice returns The Hobbit:");
        library.returnItem(hobbitCopies.get(0).getId());
        library.printCatalog();

        System.out.println("=== Scenario 3: Holds and Notifications (Observer Pattern) ===");
        System.out.println("Dune is checked out by Bob. Charlie places a hold:");
        library.placeHold(charlie.getId(), "B002");

        System.out.println("\nBob returns Dune — Charlie should be notified:");
        library.returnItem(duneCopies.get(0).getId());

        System.out.println("\nAlice tries to grab the on-hold Dune copy (should fail):");
        library.checkout(alice.getId(), duneCopies.get(0).getId());

        System.out.println("\nCharlie checks out the copy reserved for him:");
        library.checkout(charlie.getId(), duneCopies.get(0).getId());

        library.printCatalog();

        System.out.println("=== Scenario 4: Borrow Limit ===");
        Member dave = library.addMember("MEM04", "Dave", "dave@mail.com");
        for (int i = 1; i <= Member.MAX_BOOKS_PER_MEMBER; i++) {
            String copyId = "M001-c" + i;
            if (i <= 3) {
                library.checkout(dave.getId(), copyId);
            }
        }
        library.addItem(ItemType.BOOK, "B003", "Foundation", "Isaac Asimov", "978-0553293357", 1951, 5);
        library.checkout(dave.getId(), "B003-c1");
        library.checkout(dave.getId(), "B003-c2");
        System.out.println("Dave now holds " + dave.getActiveLoans().size() + " items (limit "
                + Member.MAX_BOOKS_PER_MEMBER + "). Trying one more:");
        library.checkout(dave.getId(), "B003-c3");

        System.out.println("\n=== Scenario 5: Removing an Item ===");
        library.removeItem("B002");
        library.removeItem("B001");
        library.printCatalog();
    }
}
