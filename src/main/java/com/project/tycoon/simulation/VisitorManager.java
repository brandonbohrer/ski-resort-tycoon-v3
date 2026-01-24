package com.project.tycoon.simulation;

/**
 * Manages daily visitor caps and growth tracking.
 * Visitors per day start at ~30 and grow/shrink based on satisfaction.
 */
public class VisitorManager {

    private static final int STARTING_VISITORS = 30;
    private static final int MIN_DAILY_VISITORS = 10;
    private static final int MAX_DAILY_VISITORS = 500;

    private int dailyVisitorCap = STARTING_VISITORS;
    private int visitorsSpawnedToday = 0;
    private int previousDayVisitors = 0;

    // Satisfaction tracking (placeholder for now)
    private float satisfactionScore = 0.5f; // 0.0 - 1.0

    /**
     * Check if we can spawn another visitor today.
     */
    public boolean canSpawnVisitor() {
        return visitorsSpawnedToday < dailyVisitorCap;
    }

    /**
     * Record that a visitor was spawned.
     */
    public void recordVisitorSpawned() {
        if (visitorsSpawnedToday < dailyVisitorCap) {
            visitorsSpawnedToday++;
        }
    }

    /**
     * Called at end of day to calculate growth for next day.
     */
    public void endOfDay() {
        previousDayVisitors = visitorsSpawnedToday;

        // Calculate growth based on satisfaction
        int change = calculateVisitorChange();
        dailyVisitorCap += change;

        // Clamp to min/max
        dailyVisitorCap = Math.max(MIN_DAILY_VISITORS, Math.min(MAX_DAILY_VISITORS, dailyVisitorCap));

        System.out.println("Day ended: " + visitorsSpawnedToday + " visitors. Change: " + (change >= 0 ? "+" : "")
                + change + ". Tomorrow's cap: " + dailyVisitorCap);
    }

    /**
     * Called at start of new day to reset counters.
     */
    public void startNewDay() {
        visitorsSpawnedToday = 0;
    }

    /**
     * Calculate visitor change based on satisfaction and performance.
     */
    private int calculateVisitorChange() {
        // Simple formula for now:
        // - High satisfaction (0.7+): +3 to +5 visitors
        // - Medium satisfaction (0.4-0.7): -1 to +3 visitors
        // - Low satisfaction (<0.4): -3 to -1 visitors

        if (satisfactionScore >= 0.7f) {
            return 3 + (int) (Math.random() * 3); // +3 to +5
        } else if (satisfactionScore >= 0.4f) {
            return -1 + (int) (Math.random() * 5); // -1 to +3
        } else {
            return -3 + (int) (Math.random() * 3); // -3 to -1
        }
    }

    /**
     * Update satisfaction score (0.0 - 1.0).
     * This will be calculated based on lift wait times, trail variety, etc.
     */
    public void updateSatisfaction(float score) {
        this.satisfactionScore = Math.max(0.0f, Math.min(1.0f, score));
    }

    public int getDailyVisitorCap() {
        return dailyVisitorCap;
    }

    public int getVisitorsSpawnedToday() {
        return visitorsSpawnedToday;
    }

    public int getPreviousDayVisitors() {
        return previousDayVisitors;
    }

    public float getSatisfactionScore() {
        return satisfactionScore;
    }
}
