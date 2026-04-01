package com.coffeevendingmachine.state;

import com.coffeevendingmachine.model.Coffee;
import com.coffeevendingmachine.model.CoffeeVendingMachine;

public interface VendingMachineState {
    void selectCoffee(CoffeeVendingMachine machine, Coffee coffee);
    void insertMoney(CoffeeVendingMachine machine, int amount);
    void dispenseCoffee(CoffeeVendingMachine machine);
    void cancel(CoffeeVendingMachine machine);
}
