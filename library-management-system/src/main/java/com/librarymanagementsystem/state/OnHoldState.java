package com.librarymanagementsystem.state;

import com.librarymanagementsystem.TransactionService;
import com.librarymanagementsystem.model.BookCopy;
import com.librarymanagementsystem.model.Member;

/**
 * Copy is back on the shelf but reserved — one or more members are waiting for it.
 *
 * Valid actions:
 *   checkout (by a member on the hold list) → CheckedOutState
 *   placeHold (by another member)           → stays OnHold; member joins the queue
 * Invalid action: returnItem (not currently loaned out)
 */
public class OnHoldState implements ItemState {

    @Override
    public void checkout(BookCopy copy, Member member) {
        // Authorization: only members who placed a hold may claim a held copy.
        // Walk-up borrowers must wait until the queue empties.
        if (!copy.getItem().isObserver(member)) {
            System.out.println("  " + copy.getId() + " is on hold for another member.");
            return;
        }
        // Borrow-limit check still applies, even for a hold fulfillment.
        if (!member.canBorrowMore()) {
            System.out.println("  [DENIED] " + member.getName() + " has reached the borrow limit ("
                    + Member.MAX_BOOKS_PER_MEMBER + ").");
            return;
        }
        // Fulfill the hold: create loan → remove member from waiting list → transition.
        // Removing the observer prevents double-notification if another copy returns later.
        TransactionService.getInstance().createLoan(copy, member);
        copy.getItem().removeObserver(member);
        copy.setState(new CheckedOutState());
        System.out.println("  Hold fulfilled — " + copy.getId() + " checked out by " + member.getName());
    }

    @Override
    public void returnItem(BookCopy copy) {
        // Nothing to return — a held copy isn't currently loaned. Reject the action.
        System.out.println("  Invalid action — " + copy.getId() + " is on hold, not checked out.");
    }

    @Override
    public void placeHold(BookCopy copy, Member member) {
        // Another member queues up. Stay in OnHoldState; the observer list grows.
        copy.getItem().addObserver(member);
        System.out.println("  " + member.getName() + " queued behind existing holds on '"
                + copy.getItem().getTitle() + "'");
    }
}
