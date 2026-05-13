package com.librarymanagementsystem.model;

import java.time.LocalDate;

public class Loan {
    public static final int LOAN_DURATION_DAYS = 14;

    private final BookCopy copy;
    private final Member member;
    private final LocalDate checkoutDate;
    private final LocalDate dueDate;

    public Loan(BookCopy copy, Member member) {
        this.copy = copy;
        this.member = member;
        this.checkoutDate = LocalDate.now();
        this.dueDate = this.checkoutDate.plusDays(LOAN_DURATION_DAYS);
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate);
    }

    public BookCopy getCopy() {
        return copy;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getCheckoutDate() {
        return checkoutDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
}
