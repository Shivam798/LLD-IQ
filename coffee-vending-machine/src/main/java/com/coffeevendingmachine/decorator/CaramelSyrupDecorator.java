package com.coffeevendingmachine.decorator;

import com.coffeevendingmachine.enums.Ingredient;
import com.coffeevendingmachine.model.Coffee;

import java.util.HashMap;
import java.util.Map;

public class CaramelSyrupDecorator extends CoffeeDecorator {

    private static final int COST = 30;
    private static final Map<Ingredient, Integer> RECIPE_ADDITION = Map.of(Ingredient.CARAMEL_SYRUP, 10);

    public CaramelSyrupDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getCoffeeType() {
        return decoratedCoffee.getCoffeeType() + ", Caramel Syrup";
    }

    @Override
    public int getPrice() {
        return decoratedCoffee.getPrice() + COST;
    }

    @Override
    public Map<Ingredient, Integer> getRecipe() {
        Map<Ingredient, Integer> merged = new HashMap<>(decoratedCoffee.getRecipe());
        RECIPE_ADDITION.forEach((ingredient, qty) -> merged.merge(ingredient, qty, Integer::sum));
        return merged;
    }

    @Override
    public void prepare() {
        super.prepare();
        System.out.println("- Drizzling Caramel Syrup on top.");
    }
}
