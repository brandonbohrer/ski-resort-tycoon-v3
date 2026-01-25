package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.components.SkillLevel;
import com.project.tycoon.world.model.TrailDifficulty;

/**
 * Defines trail preferences for each skill level.
 * Higher weight = stronger preference for that difficulty.
 */
public class TrailPreferences {

    /**
     * Get preference weight for a skill level on a specific trail difficulty.
     * 
     * @param skill      Skier's skill level
     * @param difficulty Trail difficulty
     * @return Preference weight (0.0 = avoid, 0.5 = acceptable, 1.0 = perfect
     *         match)
     */
    public static float getPreference(SkillLevel skill, TrailDifficulty difficulty) {
        switch (skill) {
            case BEGINNER:
                switch (difficulty) {
                    case GREEN:
                        return 0.70f; // Love greens
                    case BLUE:
                        return 0.30f; // Try some blues
                    case BLACK:
                        return 0.00f; // Avoid blacks
                    case DOUBLE_BLACK:
                        return 0.00f; // Avoid double blacks
                }
                break;

            case INTERMEDIATE:
                switch (difficulty) {
                    case GREEN:
                        return 0.20f; // Some greens
                    case BLUE:
                        return 0.60f; // Mostly blues
                    case BLACK:
                        return 0.20f; // Some blacks
                    case DOUBLE_BLACK:
                        return 0.00f; // Avoid double blacks
                }
                break;

            case ADVANCED:
                switch (difficulty) {
                    case GREEN:
                        return 0.05f; // Few greens
                    case BLUE:
                        return 0.25f; // Some blues
                    case BLACK:
                        return 0.50f; // Mostly blacks
                    case DOUBLE_BLACK:
                        return 0.20f; // Some double blacks
                }
                break;

            case EXPERT:
                switch (difficulty) {
                    case GREEN:
                        return 0.02f; // Very few greens
                    case BLUE:
                        return 0.08f; // Few blues
                    case BLACK:
                        return 0.30f; // Some blacks
                    case DOUBLE_BLACK:
                        return 0.60f; // Mostly double blacks
                }
                break;
        }

        // Default fallback
        return 0.10f;
    }

    /**
     * Check if a skill level would accept a trail difficulty.
     * 
     * @param skill      Skier's skill level
     * @param difficulty Trail difficulty
     * @return true if preference >= 0.15 (reasonable match)
     */
    public static boolean willAccept(SkillLevel skill, TrailDifficulty difficulty) {
        return getPreference(skill, difficulty) >= 0.15f;
    }

    /**
     * Check if a skill level strongly prefers a trail difficulty.
     * 
     * @param skill      Skier's skill level
     * @param difficulty Trail difficulty
     * @return true if preference >= 0.5 (strong match)
     */
    public static boolean isPreferred(SkillLevel skill, TrailDifficulty difficulty) {
        return getPreference(skill, difficulty) >= 0.5f;
    }

    /**
     * Get satisfaction gain/loss for skiing a trail.
     * 
     * @param skill      Skier's skill level
     * @param difficulty Trail they skied
     * @return Satisfaction change (-20 to +15)
     */
    public static float getSatisfactionChange(SkillLevel skill, TrailDifficulty difficulty) {
        float preference = getPreference(skill, difficulty);

        if (preference >= 0.5f) {
            return 15.0f; // Perfect match - big satisfaction boost
        } else if (preference >= 0.2f) {
            return 8.0f; // Acceptable - moderate boost
        } else if (preference >= 0.1f) {
            return 0.0f; // Barely acceptable - neutral
        } else {
            return -15.0f; // Wrong difficulty - satisfaction loss
        }
    }
}
