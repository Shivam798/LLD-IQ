package com.coffeevendingmachine.model;

import com.coffeevendingmachine.decorator.CaramelSyrupDecorator;
import com.coffeevendingmachine.decorator.ExtraSugarDecorator;
import com.coffeevendingmachine.enums.CoffeeType;
import com.coffeevendingmachine.enums.ToppingType;
import com.coffeevendingmachine.factory.CoffeeFactory;
import com.coffeevendingmachine.state.ReadyState;
import com.coffeevendingmachine.state.VendingMachineState;

import java.util.List;

public class CoffeeVendingMachine {

    private static final CoffeeVendingMachine INSTANCE = new CoffeeVendingMachine();

    private VendingMachineState state;
    private Coffee selectedCoffee;
    private int moneyInserted;

    private CoffeeVendingMachine() {
        this.state = new ReadyState();
        this.moneyInserted = 0;
    }

    public static CoffeeVendingMachine getInstance() {
        return INSTANCE;
    }

    // ── Actions delegated to current state ───────────────────────

    public void selectCoffee(CoffeeType type, List<ToppingType> toppings) {
        Coffee coffee = CoffeeFactory.createCoffee(type);

        for (ToppingType topping : toppings) {
            switch (topping) {
                case EXTRA_SUGAR:   coffee = new ExtraSugarDecorator(coffee);   break;
                case CARAMEL_SYRUP: coffee = new CaramelSyrupDecorator(coffee); break;
            }
        }

        state.selectCoffee(this, coffee);
    }

    public void insertMoney(int amount) {
        state.insertMoney(this, amount);
    }

    public void dispenseCoffee() {
        state.dispenseCoffee(this);
    }

    public void cancel() {
        state.cancel(this);
    }

    // ── State management (used by State objects) ─────────────────

    public void setState(VendingMachineState state)       { this.state = state; }
    public VendingMachineState getState()                 { return state; }
    public void setSelectedCoffee(Coffee selectedCoffee)  { this.selectedCoffee = selectedCoffee; }
    public Coffee getSelectedCoffee()                     { return selectedCoffee; }
    public void setMoneyInserted(int moneyInserted)       { this.moneyInserted = moneyInserted; }
    public int getMoneyInserted()                         { return moneyInserted; }

    public void reset() {
        this.selectedCoffee = null;
        this.moneyInserted = 0;
    }
}
