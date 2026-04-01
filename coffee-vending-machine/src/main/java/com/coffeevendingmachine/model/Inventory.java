package com.coffeevendingmachine.model;

import com.coffeevendingmachine.enums.Ingredient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Inventory {

    private static final Inventory INSTANCE = new Inventory();

    private final Map<Ingredient, Integer> stock = new ConcurrentHashMap<>();

    private Inventory() {}

    public static Inventory getInstance() {
        return INSTANCE;
    }

    public void addStock(Ingredient ingredient, int quantity) {
        stock.merge(ingredient, quantity, Integer::sum);
    }

    public boolean hasIngredients(Map<Ingredient, Integer> recipe) {
        return recipe.entrySet().stream()
                .allMatch(e -> stock.getOrDefault(e.getKey(), 0) >= e.getValue());
    }

    public synchronized void deductIngredients(Map<Ingredient, Integer> recipe) {
        if (!hasIngredients(recipe)) {
            throw new IllegalStateException("Not enough ingredients");
        }
        recipe.forEach((ingredient, qty) ->
                stock.put(ingredient, stock.get(ingredient) - qty));
    }

    public void printInventory() {
        System.out.println("--- Current Inventory ---");
        stock.forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("-------------------------");
    }
}
