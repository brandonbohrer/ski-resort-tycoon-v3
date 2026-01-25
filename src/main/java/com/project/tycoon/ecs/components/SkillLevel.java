package com.project.tycoon.ecs.components;

import java.util.Random;

/**
 * Skier skill level determining trail preferences and behavior.
 * Distribution: 20% Beginner, 30% Intermediate, 30% Advanced, 20% Expert
 */
public enum SkillLevel {
    BEGINNER(0.20), // 20% of skiers - prefer easy trails
    INTERMEDIATE(0.30), // 30% of skiers - mostly moderate trails
    ADVANCED(0.30), // 30% of skiers - challenging trails
    EXPERT(0.20); // 20% of skiers - hardest trails

    private final double spawnProbability;

    SkillLevel(double spawnProbability) {
        this.spawnProbability = spawnProbability;
    }

    public double getSpawnProbability() {
        return spawnProbability;
    }

    /**
     * Generate a random skill level based on realistic distribution.
     * 
     * @param rand Random number generator
     * @return Skill level weighted by spawn probability
     */
    public static SkillLevel randomSkill(Random rand) {
        double r = rand.nextDouble();

        // Cumulative probability distribution
        if (r < 0.20)
            return BEGINNER; // 0.00 - 0.20
        if (r < 0.50)
            return INTERMEDIATE; // 0.20 - 0.50
        if (r < 0.80)
            return ADVANCED; // 0.50 - 0.80
        return EXPERT; // 0.80 - 1.00
    }

    @Override
    public String toString() {
        switch (this) {
            case BEGINNER:
                return "Beginner";
            case INTERMEDIATE:
                return "Intermediate";
            case ADVANCED:
                return "Advanced";
            case EXPERT:
                return "Expert";
            default:
                return "Unknown";
        }
    }
}
