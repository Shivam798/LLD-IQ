package com.coffeevendingmachine.factory;

import com.coffeevendingmachine.enums.CoffeeType;
import com.coffeevendingmachine.model.Cappuccino;
import com.coffeevendingmachine.model.Coffee;
import com.coffeevendingmachine.model.Espresso;
import com.coffeevendingmachine.model.Latte;

public class CoffeeFactory {

    private CoffeeFactory() {}

    public static Coffee createCoffee(CoffeeType type) {
        switch (type) {
            case ESPRESSO:   return new Espresso();
            case LATTE:      return new Latte();
            case CAPPUCCINO: return new Cappuccino();
            default:
                throw new IllegalArgumentException("Unsupported coffee type: " + type);
        }
    }
}
