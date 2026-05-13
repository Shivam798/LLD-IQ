package com.librarymanagementsystem.model;

import com.librarymanagementsystem.observer.HoldObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Member implements HoldObserver {
    public static final int MAX_BOOKS_PER_MEMBER = 5;

    private final String id;
    private final String name;
    private final String contactInfo;
    private final List<Loan> activeLoans = new CopyOnWriteArrayList<>();
    private final List<Loan> borrowingHistory = new CopyOnWriteArrayList<>();

    public Member(String id, String name, String contactInfo) {
        this.id = id;
        this.name = name;
        this.contactInfo = contactInfo;
    }

    @Override
    public void update(LibraryItem item) {
        System.out.println("  [NOTIFY " + name + "] '" + item.getTitle()
                + "' you placed a hold on is now available for checkout.");
    }

    public boolean canBorrowMore() {
        return activeLoans.size() < MAX_BOOKS_PER_MEMBER;
    }

    public void addLoan(Loan loan) {
        activeLoans.add(loan);
        borrowingHistory.add(loan);
    }

    public void removeLoan(Loan loan) {
        activeLoans.remove(loan);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public List<Loan> getActiveLoans() {
        return activeLoans;
    }

    public List<Loan> getBorrowingHistory() {
        return borrowingHistory;
    }
}
