package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;
import java.util.UUID;

public class LiftComponent implements Component {
    public enum LiftType {
        TBAR, CHAIRLIFT, GONDOLA
    }

    public LiftType type;
    public float speed;
    public int capacity; // Max riders simultaneously
    public float maintenanceCostPerSec; // Operational cost per second
    public UUID nextPylonId; // For linked list of pylons

    public LiftComponent(LiftType type) {
        this.type = type;

        // Set stats based on type
        switch (type) {
            case TBAR:
                this.speed = 2.0f;
                this.capacity = 8;
                this.maintenanceCostPerSec = 0.05f;
                break;
            case CHAIRLIFT:
                this.speed = 2.5f;
                this.capacity = 12;
                this.maintenanceCostPerSec = 0.15f;
                break;
            case GONDOLA:
                this.speed = 3.0f;
                this.capacity = 20;
                this.maintenanceCostPerSec = 0.30f;
                break;
        }

        this.nextPylonId = null;
    }

    /**
     * Get the base construction cost for a lift type.
     */
    public static float getBaseCost(LiftType type) {
        switch (type) {
            case TBAR:
                return 2000f;
            case CHAIRLIFT:
                return 8000f;
            case GONDOLA:
                return 20000f;
            default:
                return 0f;
        }
    }

    /**
     * Get the per-pylon construction cost for a lift type.
     */
    public static float getCostPerPylon(LiftType type) {
        switch (type) {
            case TBAR:
                return 150f;
            case CHAIRLIFT:
                return 400f;
            case GONDOLA:
                return 800f;
            default:
                return 0f;
        }
    }

    /**
     * Calculate total construction cost for a lift.
     */
    public static float calculateTotalCost(LiftType type, int pylonCount) {
        return getBaseCost(type) + (getCostPerPylon(type) * pylonCount);
    }
}