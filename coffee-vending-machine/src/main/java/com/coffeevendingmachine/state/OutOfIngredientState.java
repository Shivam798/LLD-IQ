package com.coffeevendingmachine.state;

import com.coffeevendingmachine.model.Coffee;
import com.coffeevendingmachine.model.CoffeeVendingMachine;

public class OutOfIngredientState implements VendingMachineState {

    @Override
    public void selectCoffee(CoffeeVendingMachine machine, Coffee coffee) {
        System.out.println("Sorry, the machine is out of ingredients.");
    }

    @Override
    public void insertMoney(CoffeeVendingMachine machine, int amount) {
        System.out.println("Sorry, the machine is out of ingredients. Money refunded.");
    }

    @Override
    public void dispenseCoffee(CoffeeVendingMachine machine) {
        System.out.println("Sorry, the machine is out of ingredients.");
    }

    @Override
    public void cancel(CoffeeVendingMachine machine) {
        System.out.println("Refunding " + machine.getMoneyInserted());
        machine.reset();
        machine.setState(new ReadyState());
    }
}
