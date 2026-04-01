package com.coffeevendingmachine.model;

import com.coffeevendingmachine.enums.Ingredient;

import java.util.Map;

/**
 * Abstract base for all coffees.
 *
 * Serves two roles:
 *   1. Template Method  — prepare() defines the skeleton; addCondiments() is the hook.
 *   2. Decorator Component — decorators wrap this to add toppings / extra cost.
 */
public abstract class Coffee {

    protected String coffeeType = "Unknown Coffee";

    public String getCoffeeType() {
        return coffeeType;
    }

    // ── Template Method ──────────────────────────────────────────
    public void prepare() {
        System.out.println("\nPreparing your " + getCoffeeType() + "...");
        grindBeans();
        brew();
        addCondiments();
        pourIntoCup();
        System.out.println(getCoffeeType() + " is ready!");
    }

    private void grindBeans()  { System.out.println("- Grinding fresh coffee beans."); }
    private void brew()        { System.out.println("- Brewing coffee with hot water."); }
    private void pourIntoCup() { System.out.println("- Pouring into a cup."); }

    // Hook — subclasses override to add milk, foam, etc.
    protected abstract void addCondiments();

    // ── Pricing & Recipe ─────────────────────────────────────────
    public abstract int getPrice();
    public abstract Map<Ingredient, Integer> getRecipe();
}
