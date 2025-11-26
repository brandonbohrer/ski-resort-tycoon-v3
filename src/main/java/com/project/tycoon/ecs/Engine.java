package com.project.tycoon.ecs;

import java.util.*;

/**
 * The ECS Engine managing entities, components, and systems.
 */
public class Engine {
    private final Set<Entity> entities = new HashSet<>();
    private final Map<UUID, Map<Class<? extends Component>, Component>> components = new HashMap<>();
    private final List<System> systems = new ArrayList<>();

    /**
     * Creates and registers a new entity.
     * @return The created Entity.
     */
    public Entity createEntity() {
        Entity entity = new Entity();
        entities.add(entity);
        components.put(entity.getId(), new HashMap<>());
        return entity;
    }

    /**
     * Adds a component to an entity.
     */
    public <T extends Component> void addComponent(Entity entity, T component) {
        if (!entities.contains(entity)) {
            throw new IllegalArgumentException("Entity does not exist in this engine.");
        }
        components.get(entity.getId()).put(component.getClass(), component);
    }

    /**
     * Retrieves a component of a specific type for an entity.
     */
    public <T extends Component> T getComponent(Entity entity, Class<T> componentClass) {
        if (!entities.contains(entity)) {
            throw new IllegalArgumentException("Entity does not exist in this engine.");
        }
        return componentClass.cast(components.get(entity.getId()).get(componentClass));
    }
    
    /**
     * Checks if an entity has a component.
     */
     public boolean hasComponent(Entity entity, Class<? extends Component> componentClass) {
        if (!entities.contains(entity)) return false;
        return components.get(entity.getId()).containsKey(componentClass);
     }

    /**
     * Registers a system to be updated by the engine.
     */
    public void addSystem(System system) {
        systems.add(system);
    }

    /**
     * Updates all registered systems.
     * @param dt Time delta.
     */
    public void update(double dt) {
        for (System system : systems) {
            system.update(dt);
        }
    }
    
    /**
     * Returns a view of all entities.
     */
    public Set<Entity> getEntities() {
        return Collections.unmodifiableSet(entities);
    }
}

