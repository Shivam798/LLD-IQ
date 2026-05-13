package com.librarymanagementsystem.state;

import com.librarymanagementsystem.TransactionService;
import com.librarymanagementsystem.model.BookCopy;
import com.librarymanagementsystem.model.Member;

/**
 * Copy is currently with a borrower.
 *
 * Valid actions:
 *   returnItem → AvailableState (or OnHoldState if anyone is waiting)
 *   placeHold  → stays CheckedOut; member joins the waiting list
 * Invalid action: checkout (already taken)
 */
public class CheckedOutState implements ItemState {

    @Override
    public void checkout(BookCopy copy, Member member) {
        // Already borrowed by someone else — reject. If this member wants it,
        // they should placeHold() instead.
        System.out.println("  " + copy.getId() + " is already checked out.");
    }

    @Override
    public void returnItem(BookCopy copy) {
        // End the loan record first (frees the borrower's slot, computes fines, etc.).
        TransactionService.getInstance().endLoan(copy);
        System.out.println("  " + copy.getId() + " returned.");

        // Transition depends on whether anyone is waiting:
        //   - holders exist → go to OnHoldState and notify them (Observer pattern)
        //   - no holders   → straight back to AvailableState (shelf-ready)
        if (copy.getItem().hasObservers()) {
            copy.setState(new OnHoldState());
            copy.getItem().notifyObservers();
        } else {
            copy.setState(new AvailableState());
        }
    }

    @Override
    public void placeHold(BookCopy copy, Member member) {
        // Register the member as a watcher on the parent item (not the specific copy)
        // so they get notified when ANY copy of that title becomes available.
        copy.getItem().addObserver(member);
        System.out.println("  " + member.getName() + " placed a hold on '" + copy.getItem().getTitle() + "'");
    }
}
