package com.librarymanagementsystem.state;

import com.librarymanagementsystem.model.BookCopy;
import com.librarymanagementsystem.model.Member;

/**
 * State pattern interface — defines the actions a {@link BookCopy} can undergo.
 *
 * Each concrete state (Available, CheckedOut, OnHold) decides what happens when
 * an action is invoked while the copy is in that state. Some actions cause a
 * transition (e.g. checkout from Available → CheckedOut); others are rejected
 * (e.g. returnItem from Available — nothing to return).
 *
 * Implementations are intentionally stateless so a single instance can be shared
 * across many BookCopy objects — the context (copy, member) is passed in per call.
 */
public interface ItemState {

    // Borrower wants to take the copy home. Member is required to authorize
    // (borrow-limit check) and to record who holds the loan.
    void checkout(BookCopy copy, Member member);

    // Copy is being returned to the library. No Member needed — the loan record
    // already knows the borrower; anyone can drop the book at the desk.
    void returnItem(BookCopy copy);

    // Member wants to reserve the copy for later (only meaningful when it's
    // currently unavailable). Member is required so they can be notified.
    void placeHold(BookCopy copy, Member member);
}
