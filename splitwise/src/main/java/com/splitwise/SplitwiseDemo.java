package com.splitwise;

import com.splitwise.model.Expense;
import com.splitwise.model.Group;
import com.splitwise.model.Transaction;
import com.splitwise.model.User;
import com.splitwise.strategy.EqualSplitStrategy;
import com.splitwise.strategy.ExactSplitStrategy;
import com.splitwise.strategy.PercentageSplitStrategy;

import java.util.Arrays;
import java.util.List;

/**
 * End-to-end walkthrough of the Splitwise system:
 *   1. Set up users + a group
 *   2. Add expenses using all three split strategies (Equal, Exact, Percentage)
 *   3. Inspect balance sheets after each
 *   4. Run debt simplification on the group
 *   5. Partial settle-up and re-check balances
 *
 * Each section is annotated so you can scan the demo and see exactly which
 * piece of the design is being exercised.
 */
public class SplitwiseDemo {
    public static void main(String[] args) {
        // 1. Bootstrap the singleton service
        SplitwiseService service = SplitwiseService.getInstance();

        // 2. Register users and form a group
        User alice = service.addUser("Alice", "alice@a.com");
        User bob = service.addUser("Bob", "bob@b.com");
        User charlie = service.addUser("Charlie", "charlie@c.com");
        User david = service.addUser("David", "david@d.com");

        Group friendsGroup = service.addGroup("Friends Trip", List.of(alice, bob, charlie, david));

        System.out.println("--- System Setup Complete ---\n");

        // 3. Equal split — Alice pays $1000 for dinner, all four share equally
        System.out.println("--- Use Case 1: Equal Split ---");
        service.createExpense(new Expense.ExpenseBuilder()
                .setDescription("Dinner")
                .setAmount(1000)
                .setPaidBy(alice)
                .setParticipants(Arrays.asList(alice, bob, charlie, david))
                .setSplitStrategy(new EqualSplitStrategy()));

        service.showBalanceSheet(alice.getId());
        service.showBalanceSheet(bob.getId());
        System.out.println();

        // 4. Exact split — Alice pays $370 for movie tickets; Bob owes $120, Charlie owes $250
        System.out.println("--- Use Case 2: Exact Split ---");
        service.createExpense(new Expense.ExpenseBuilder()
                .setDescription("Movie Tickets")
                .setAmount(370)
                .setPaidBy(alice)
                .setParticipants(Arrays.asList(bob, charlie))
                .setSplitStrategy(new ExactSplitStrategy())
                .setSplitValues(Arrays.asList(120.0, 250.0)));

        service.showBalanceSheet(alice.getId());
        service.showBalanceSheet(bob.getId());
        System.out.println();

        // 5. Percentage split — David pays $500 for groceries; Alice 40%, Bob 30%, Charlie 30%
        System.out.println("--- Use Case 3: Percentage Split ---");
        service.createExpense(new Expense.ExpenseBuilder()
                .setDescription("Groceries")
                .setAmount(500)
                .setPaidBy(david)
                .setParticipants(Arrays.asList(alice, bob, charlie))
                .setSplitStrategy(new PercentageSplitStrategy())
                .setSplitValues(Arrays.asList(40.0, 30.0, 30.0)));

        System.out.println("--- Balances After All Expenses ---");
        service.showBalanceSheet(alice.getId());
        service.showBalanceSheet(bob.getId());
        service.showBalanceSheet(charlie.getId());
        service.showBalanceSheet(david.getId());
        System.out.println();

        // 6. Simplify the tangle: greedy minimum-cash-flow within the group
        System.out.println("--- Use Case 4: Simplify Group Debts for 'Friends Trip' ---");
        List<Transaction> simplifiedDebts = service.simplifyGroupDebts(friendsGroup.getId());
        if (simplifiedDebts.isEmpty()) {
            System.out.println("All debts are settled within the group!");
        } else {
            simplifiedDebts.forEach(System.out::println);
        }
        System.out.println();

        // 7. Partial settlement — Bob hands Alice $100 cash
        System.out.println("--- Use Case 5: Partial Settlement ---");
        service.settleUp(bob.getId(), alice.getId(), 100);

        System.out.println("--- Balances After Partial Settlement ---");
        service.showBalanceSheet(alice.getId());
        service.showBalanceSheet(bob.getId());
    }
}
