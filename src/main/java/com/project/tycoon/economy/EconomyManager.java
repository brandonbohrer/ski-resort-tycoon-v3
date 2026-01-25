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

    // Daily revenue tracking (batch system)
    private float pendingDailyRevenue; // Accumulates during day
    private float lastDailyRevenue; // Yesterday's revenue for display

    // Configuration constants
    public static final float STARTING_MONEY = 500000f;
    public static final float TICKET_PRICE = 8f;

    public EconomyManager() {
        this.currentMoney = STARTING_MONEY;
        this.totalRevenue = 0f;
        this.totalExpenses = 0f;
        this.pendingDailyRevenue = 0f;
        this.lastDailyRevenue = 0f;
    }

    /**
     * Record a ticket sale (does NOT apply revenue immediately).
     * Revenue is applied at end of day.
     */
    public void recordTicketSale() {
        pendingDailyRevenue += TICKET_PRICE;
    }

    /**
     * Deduct operational expenses.
     */
    public void deductExpense(float amount) {
        currentMoney -= amount;
        totalExpenses += amount;
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
     * Called at end of each day to apply batch revenue.
     * 
     * @return The revenue earned this day
     */
    public float endOfDay() {
        // Apply pending revenue
        currentMoney += pendingDailyRevenue;
        totalRevenue += pendingDailyRevenue;

        // Store for display
        lastDailyRevenue = pendingDailyRevenue;

        // Reset for next day
        float revenue = pendingDailyRevenue;
        pendingDailyRevenue = 0f;

        return revenue;
    }

    /**
     * Update method (minimal now that we don't track per-second revenue).
     */
    public void update(double dt) {
        // Placeholder for future per-second expenses if needed
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

    public float getPendingDailyRevenue() {
        return pendingDailyRevenue;
    }

    public float getLastDailyRevenue() {
        return lastDailyRevenue;
    }
}
