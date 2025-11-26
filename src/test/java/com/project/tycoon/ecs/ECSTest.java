package com.project.tycoon.ecs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ECSTest {

    // Test component
    static class Position implements Component {
        public int x, y;
        public Position(int x, int y) { this.x = x; this.y = y; }
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
}

