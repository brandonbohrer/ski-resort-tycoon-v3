package com.project.tycoon.ecs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ECSTest {

    // Test component
    static class Position implements Component {
        public int x, y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @Test
    void testEntityCreationAndComponents() {
        Engine engine = new Engine();
        Entity entity = engine.createEntity();

        assertNotNull(entity.getId());

        Position pos = new Position(10, 20);
        engine.addComponent(entity, pos);

        assertTrue(engine.hasComponent(entity, Position.class));
        assertEquals(pos, engine.getComponent(entity, Position.class));
    }

    @Test
    void testSystemUpdate() {
        Engine engine = new Engine();

        // Mock system
        class CounterSystem implements System {
            int updates = 0;

            @Override
            public void update(double dt) {
                updates++;
            }
        }

        CounterSystem system = new CounterSystem();
        engine.addSystem(system);

        engine.update(0.1);
        engine.update(0.1);

        assertEquals(2, system.updates);
    }

    @Test
    void testEntityRemoval() {
        Engine engine = new Engine();
        Entity entity = engine.createEntity();
        Position pos = new Position(10, 20);
        engine.addComponent(entity, pos);

        // Verify entity exists with component
        assertTrue(engine.hasComponent(entity, Position.class));
        assertTrue(engine.getEntities().contains(entity));

        // Remove entity
        engine.removeEntity(entity);

        // Verify entity is gone
        assertFalse(engine.getEntities().contains(entity));
        assertFalse(engine.hasComponent(entity, Position.class));
    }

    @Test
    void testComponentRemoval() {
        Engine engine = new Engine();
        Entity entity = engine.createEntity();

        // Add multiple components
        Position pos = new Position(10, 20);
        Velocity vel = new Velocity(5, 5);
        engine.addComponent(entity, pos);
        engine.addComponent(entity, vel);

        // Verify both exist
        assertTrue(engine.hasComponent(entity, Position.class));
        assertTrue(engine.hasComponent(entity, Velocity.class));

        // Remove one component
        Position removed = engine.removeComponent(entity, Position.class);

        // Verify correct component was removed
        assertEquals(pos, removed);
        assertFalse(engine.hasComponent(entity, Position.class));

        // Verify entity still exists with other component
        assertTrue(engine.getEntities().contains(entity));
        assertTrue(engine.hasComponent(entity, Velocity.class));
    }

    @Test
    void testRemoveNonexistentEntity() {
        Engine engine = new Engine();
        Entity entity = new Entity(); // Not added to engine

        // Should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            engine.removeEntity(entity);
        });
    }

    @Test
    void testRemoveComponentFromNonexistentEntity() {
        Engine engine = new Engine();
        Entity entity = new Entity(); // Not added to engine

        // Should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            engine.removeComponent(entity, Position.class);
        });
    }

    // Additional test component
    static class Velocity implements Component {
        public int dx, dy;

        public Velocity(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }
}
