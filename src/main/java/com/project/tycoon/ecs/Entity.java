package com.project.tycoon.ecs;

import java.util.UUID;

/**
 * Represents a distinct object in the game world.
 * An entity is defined solely by its unique ID and the components attached to it.
 */
public class Entity {
    private final UUID id;

    public Entity() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

