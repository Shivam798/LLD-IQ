package com.coffeevendingmachine;

import com.coffeevendingmachine.enums.CoffeeType;
import com.coffeevendingmachine.enums.Ingredient;
import com.coffeevendingmachine.enums.ToppingType;
import com.coffeevendingmachine.model.CoffeeVendingMachine;
import com.coffeevendingmachine.model.Inventory;

import java.util.List;

public class CoffeeVendingMachineDemo {

    public static void main(String[] args) {

        CoffeeVendingMachine machine = CoffeeVendingMachine.getInstance();
        Inventory inventory = Inventory.getInstance();

        // ── Initialize inventory ──────────────────────────────────
        System.out.println("=== Initializing Vending Machine ===");
        inventory.addStock(Ingredient.COFFEE_BEANS, 50);
        inventory.addStock(Ingredient.WATER, 500);
        inventory.addStock(Ingredient.MILK, 200);
        inventory.addStock(Ingredient.SUGAR, 100);
        inventory.addStock(Ingredient.CARAMEL_SYRUP, 50);
        inventory.printInventory();

        // ── Scenario 1: Successful Latte purchase ─────────────────
        System.out.println("\n--- SCENARIO 1: Buy a Latte (Success) ---");
        machine.selectCoffee(CoffeeType.LATTE, List.of());
        machine.insertMoney(200);
        machine.insertMoney(50);    // Total 250, price is 220 → change 30
        machine.dispenseCoffee();
        inventory.printInventory();

        // ── Scenario 2: Insufficient funds → cancel ───────────────
        System.out.println("\n--- SCENARIO 2: Buy Espresso (Insufficient Funds & Cancel) ---");
        machine.selectCoffee(CoffeeType.ESPRESSO, List.of());
        machine.insertMoney(100);   // Price is 150 — not enough
        machine.dispenseCoffee();    // Should fail
        machine.cancel();            // Refund 100
        inventory.printInventory();

        // ── Scenario 3: Out of ingredients ────────────────────────
        System.out.println("\n--- SCENARIO 3: Buy Cappuccino with toppings (Out of Milk) ---");
        machine.selectCoffee(CoffeeType.CAPPUCCINO, List.of(ToppingType.CARAMEL_SYRUP, ToppingType.EXTRA_SUGAR));
        machine.insertMoney(300);
        machine.dispenseCoffee();    // Should fail — insufficient milk
        inventory.printInventory();

        // ── Scenario 4: Refill and buy ────────────────────────────
        System.out.println("\n--- SCENARIO 4: Refill & Buy Latte with Caramel ---");
        inventory.addStock(Ingredient.MILK, 200);
        machine.selectCoffee(CoffeeType.LATTE, List.of(ToppingType.CARAMEL_SYRUP));
        machine.insertMoney(250);   // Latte(220) + Caramel(30) = 250
        machine.dispenseCoffee();
        inventory.printInventory();
    }
}
