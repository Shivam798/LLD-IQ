package com.librarymanagementsystem;

import com.librarymanagementsystem.model.BookCopy;
import com.librarymanagementsystem.model.Loan;
import com.librarymanagementsystem.model.Member;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionService {
    private static final TransactionService INSTANCE = new TransactionService();

    private final Map<String, Loan> activeLoans = new ConcurrentHashMap<>();

    private TransactionService() {}

    public static TransactionService getInstance() {
        return INSTANCE;
    }

    public void createLoan(BookCopy copy, Member member) {
        if (activeLoans.containsKey(copy.getId())) {
            throw new IllegalStateException("Copy " + copy.getId() + " is already on loan.");
        }
        Loan loan = new Loan(copy, member);
        activeLoans.put(copy.getId(), loan);
        member.addLoan(loan);
    }

    public void endLoan(BookCopy copy) {
        Loan loan = activeLoans.remove(copy.getId());
        if (loan != null) {
            loan.getMember().removeLoan(loan);
        }
    }

    public Loan getActiveLoan(String copyId) {
        return activeLoans.get(copyId);
    }
}
