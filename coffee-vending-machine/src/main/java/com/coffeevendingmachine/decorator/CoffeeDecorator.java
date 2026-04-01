package com.coffeevendingmachine.decorator;

import com.coffeevendingmachine.enums.Ingredient;
import com.coffeevendingmachine.model.Coffee;

import java.util.Map;

/**
 * Base decorator — delegates everything to the wrapped Coffee.
 * Concrete decorators override methods to add extra behavior/cost/ingredients.
 */
public abstract class CoffeeDecorator extends Coffee {

    protected final Coffee decoratedCoffee;

    protected CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public int getPrice() {
        return decoratedCoffee.getPrice();
    }

    @Override
    public Map<Ingredient, Integer> getRecipe() {
        return decoratedCoffee.getRecipe();
    }

    @Override
    protected void addCondiments() {
        // No-op — preparation is fully delegated through prepare()
    }

    @Override
    public void prepare() {
        decoratedCoffee.prepare();
    }
}
