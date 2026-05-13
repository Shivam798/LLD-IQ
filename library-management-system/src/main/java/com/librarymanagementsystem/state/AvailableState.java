package com.librarymanagementsystem.state;

import com.librarymanagementsystem.TransactionService;
import com.librarymanagementsystem.model.BookCopy;
import com.librarymanagementsystem.model.Member;

/**
 * Copy is on the shelf and free to borrow.
 *
 * Valid action:   checkout  → CheckedOutState
 * Invalid actions: returnItem (nothing to return), placeHold (just borrow it directly)
 */
public class AvailableState implements ItemState {

    @Override
    public void checkout(BookCopy copy, Member member) {
        // Guard: enforce per-member borrow limit before creating the loan.
        if (!member.canBorrowMore()) {
            System.out.println("  [DENIED] " + member.getName() + " has reached the borrow limit ("
                    + Member.MAX_BOOKS_PER_MEMBER + ").");
            return;
        }
        // 1) Record the loan, 2) flip the copy's state. Order matters: if the
        // transaction fails we don't want a copy stuck in CheckedOut with no loan.
        TransactionService.getInstance().createLoan(copy, member);
        copy.setState(new CheckedOutState());
        System.out.println("  " + copy.getId() + " checked out by " + member.getName());
    }

    @Override
    public void returnItem(BookCopy copy) {
        // Can't return what was never borrowed — reject without state change.
        System.out.println("  Cannot return " + copy.getId() + " — it is already available.");
    }

    @Override
    public void placeHold(BookCopy copy, Member member) {
        // Holds only make sense for unavailable copies. If it's on the shelf,
        // the member should just check it out instead of queueing for it.
        System.out.println("  Cannot place hold on " + copy.getId() + " — it is available. Please check it out.");
    }
}
