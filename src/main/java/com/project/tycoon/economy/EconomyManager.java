package com.project.tycoon.economy;

/**
 * Central manager for all financial transactions and economic tracking.
 * Handles revenue from ticket sales, operational expenses, and money balance.
 */
public class EconomyManager {

    // Current state
    private float currentMoney;
    private float totalRevenue;
    private float totalExpenses;

    // Per-second tracking for display
    private float revenueThisCycle;
    private float expensesThisCycle;
    private float cycleTimer;
    private float revenuePerSecond;
    private float expensesPerSecond;

    // Configuration constants
    public static final float STARTING_MONEY = 15000f;
    public static final float TICKET_PRICE = 8f;

    public EconomyManager() {
        this.currentMoney = STARTING_MONEY;
        this.totalRevenue = 0f;
        this.totalExpenses = 0f;
        this.revenueThisCycle = 0f;
        this.expensesThisCycle = 0f;
        this.cycleTimer = 0f;
        this.revenuePerSecond = 0f;
        this.expensesPerSecond = 0f;
    }

    /**
     * Add revenue from ticket sales or other sources.
     */
    public void addRevenue(float amount) {
        currentMoney += amount;
        totalRevenue += amount;
        revenueThisCycle += amount;
    }

    /**
     * Deduct operational expenses.
     */
    public void deductExpense(float amount) {
        currentMoney -= amount;
        totalExpenses += amount;
        expensesThisCycle += amount;
    }

    /**
     * Check if player can afford a purchase.
     */
    public boolean canAfford(float amount) {
        return currentMoney >= amount;
    }

    /**
     * Make a purchase (construction, etc). Returns true if successful.
     */
    public boolean purchase(float amount) {
        if (!canAfford(amount)) {
            return false;
        }
        currentMoney -= amount;
        totalExpenses += amount;
        return true;
    }

    /**
     * Update per-second calculations for display.
     */
    public void update(double dt) {
        cycleTimer += dt;

        // Update rates every second
        if (cycleTimer >= 1.0) {
            revenuePerSecond = revenueThisCycle / (float) cycleTimer;
            expensesPerSecond = expensesThisCycle / (float) cycleTimer;

            // Reset cycle
            revenueThisCycle = 0f;
            expensesThisCycle = 0f;
            cycleTimer = 0f;
        }
    }

    // Getters
    public float getCurrentMoney() {
        return currentMoney;
    }

    public float getTotalRevenue() {
        return totalRevenue;
    }

    public float getTotalExpenses() {
        return totalExpenses;
    }

    public float getRevenuePerSecond() {
        return revenuePerSecond;
    }

    public float getExpensesPerSecond() {
        return expensesPerSecond;
    }

    public float getNetIncomePerSecond() {
        return revenuePerSecond - expensesPerSecond;
    }
}
