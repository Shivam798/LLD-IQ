package com.coffeevendingmachine.state;

import com.coffeevendingmachine.model.Coffee;
import com.coffeevendingmachine.model.CoffeeVendingMachine;
import com.coffeevendingmachine.model.Inventory;

public class PaidState implements VendingMachineState {

    @Override
    public void selectCoffee(CoffeeVendingMachine machine, Coffee coffee) {
        System.out.println("Cannot select another coffee now.");
    }

    @Override
    public void insertMoney(CoffeeVendingMachine machine, int amount) {
        System.out.println("Already paid. Please wait for your coffee.");
    }

    @Override
    public void dispenseCoffee(CoffeeVendingMachine machine) {
        Inventory inventory = Inventory.getInstance();
        Coffee coffee = machine.getSelectedCoffee();

        if (!inventory.hasIngredients(coffee.getRecipe())) {
            System.out.println("Sorry, out of ingredients for " + coffee.getCoffeeType());
            machine.setState(new OutOfIngredientState());
            machine.getState().cancel(machine);
            return;
        }

        inventory.deductIngredients(coffee.getRecipe());
        coffee.prepare();

        int change = machine.getMoneyInserted() - coffee.getPrice();
        if (change > 0) {
            System.out.println("Returning change: " + change);
        }

        machine.reset();
        machine.setState(new ReadyState());
    }

    @Override
    public void cancel(CoffeeVendingMachine machine) {
        System.out.println("Transaction cancelled. Refunding " + machine.getMoneyInserted());
        machine.reset();
        machine.setState(new ReadyState());
    }
}
