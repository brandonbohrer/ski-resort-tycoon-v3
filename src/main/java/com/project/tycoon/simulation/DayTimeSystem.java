package com.project.tycoon.simulation;

/**
 * Manages the day/night cycle for the ski resort.
 * Operating hours: 9:00 AM - 5:00 PM (8 hours)
 * Nights are skipped entirely.
 */
public class DayTimeSystem {

    private static final float HOURS_PER_DAY = 8.0f; // 9 AM to 5 PM
    private static final int START_HOUR = 9; // 9 AM
    private static final int END_HOUR = 17; // 5 PM

    private int currentDay = 1;
    private float timeOfDay = 0.0f; // 0.0 = 9 AM, 8.0 = 5 PM

    private DayTransitionListener dayTransitionListener;

    public interface DayTransitionListener {
        void onDayEnd(int dayNumber);

        void onDayStart(int dayNumber);
    }

    /**
     * Update the time system.
     * 
     * @param dt Delta time in seconds
     */
    public void update(double dt) {
        // Convert real seconds to game hours
        // For reference: 1x speed should make 1 game day = 2 minutes real time
        // 8 hours / 120 seconds = 0.0667 hours per second
        float hoursPerSecond = HOURS_PER_DAY / 120.0f;

        timeOfDay += (float) dt * hoursPerSecond;

        // Check if day ended
        if (timeOfDay >= HOURS_PER_DAY) {
            endDay();
            startNewDay();
        }
    }

    private void endDay() {
        if (dayTransitionListener != null) {
            dayTransitionListener.onDayEnd(currentDay);
        }
    }

    private void startNewDay() {
        currentDay++;
        timeOfDay = 0.0f;

        if (dayTransitionListener != null) {
            dayTransitionListener.onDayStart(currentDay);
        }
    }

    /**
     * Get current day number (1-indexed).
     */
    public int getCurrentDay() {
        return currentDay;
    }

    /**
     * Get current time of day as a formatted string (e.g., "2:30 PM").
     */
    public String getTimeString() {
        int hour = START_HOUR + (int) timeOfDay;
        int minute = (int) ((timeOfDay % 1.0f) * 60.0f);

        String period = hour < 12 ? "AM" : "PM";
        int displayHour = hour > 12 ? hour - 12 : hour;

        return String.format("%d:%02d %s", displayHour, minute, period);
    }

    /**
     * Get time of day as hours since 9 AM (0.0 - 8.0).
     */
    public float getTimeOfDay() {
        return timeOfDay;
    }

    /**
     * Get current hour (9-17).
     */
    public int getCurrentHour() {
        return START_HOUR + (int) timeOfDay;
    }

    /**
     * Check if resort is currently open (should always be true during simulation).
     */
    public boolean isResortOpen() {
        return timeOfDay >= 0.0f && timeOfDay < HOURS_PER_DAY;
    }

    /**
     * Set listener for day transition events.
     */
    public void setDayTransitionListener(DayTransitionListener listener) {
        this.dayTransitionListener = listener;
    }

    /**
     * Get progress through the day (0.0 - 1.0).
     */
    public float getDayProgress() {
        return timeOfDay / HOURS_PER_DAY;
    }
}
