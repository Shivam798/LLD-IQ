package com.coffeevendingmachine.state;

import com.coffeevendingmachine.model.Coffee;
import com.coffeevendingmachine.model.CoffeeVendingMachine;

public class ReadyState implements VendingMachineState {

    @Override
    public void selectCoffee(CoffeeVendingMachine machine, Coffee coffee) {
        machine.setSelectedCoffee(coffee);
        machine.setState(new SelectingState());
        System.out.println(coffee.getCoffeeType() + " selected. Price: " + coffee.getPrice());
    }

    @Override
    public void insertMoney(CoffeeVendingMachine machine, int amount) {
        System.out.println("Please select a coffee first.");
    }

    @Override
    public void dispenseCoffee(CoffeeVendingMachine machine) {
        System.out.println("Please select and pay first.");
    }

    @Override
    public void cancel(CoffeeVendingMachine machine) {
        System.out.println("Nothing to cancel.");
    }
}
