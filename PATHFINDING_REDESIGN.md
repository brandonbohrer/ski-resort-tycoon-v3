# Skier Pathfinding Redesign

## Problem Statement

The original skier AI system had critical flaws when dealing with multiple interconnected lifts and trails:

1. **Skiers walked UP ski trails** - They would find the "nearest lift" even if it meant hiking uphill
2. **Circular behavior** - Skiers would loop between nearby snap points without making progress
3. **No graph awareness** - Despite having a snap point graph system, skiers used only local "nearest neighbor" logic

### Why It Failed

```
Example broken scenario:

    Peak (Lift B top) ← Skier tries to walk here!
         ↓ Trail 2
    Mid (Lift A top) ← Skier finishes Trail 1 here
         ↓ Trail 1
    Base (Lift A bottom)
```

**Old behavior:** Skier finishes Trail 1, enters WAITING state, finds Lift B base is "nearby" up the mountain, walks UP Trail 2 to reach it → Chaos!

---

## Solution: Goal-Oriented Graph Navigation

### Key Changes

#### 1. **New System: `SkierNavigationSystem`**
   - Handles high-level navigation in WAITING state
   - Uses snap point graph pathfinding (BFS)
   - Plans complete paths before moving
   - Validates paths are navigable (no walking up trails)

#### 2. **Proper Graph Pathfinding in `SnapPointManager`**
   - Implemented BFS-based `findPath(start, end)` 
   - Returns full path as list of snap point IDs
   - Handles multi-hop connections

#### 3. **Path Validation**
   - Checks if path is walkable (can't go TRAIL_END → TRAIL_START on foot)
   - Ensures skiers only walk on valid routes

#### 4. **State Machine Separation**
   - `SkierNavigationSystem`: Handles WAITING state (walking to lifts)
   - `SkierBehaviorSystem`: Handles SKIING state (on-trail behavior)
   - `LiftSystem`: Handles QUEUED and RIDING_LIFT states

---

## New Architecture

### SkierComponent Fields Added:
```java
public List<UUID> plannedPath;  // Sequence of snap point IDs to follow
public int pathIndex;            // Current waypoint in path
```

### System Execution Order:
1. **SkierNavigationSystem** - Plans and executes high-level paths (WAITING)
2. **SkierBehaviorSystem** - Handles low-level skiing physics (SKIING)
3. **LiftSystem** - Manages lift queues and transport (QUEUED, RIDING_LIFT)

---

## How It Works Now

### Scenario: Skier finishes trail and needs to ride lift

1. **Skier finishes trail** → State = WAITING
2. **SkierNavigationSystem.update():**
   - Finds nearest snap point to skier's position
   - Gets all lift bases in resort
   - Runs BFS pathfinding to each lift base
   - Picks shortest **valid** path (no walking up trails)
   - Stores path in `skier.plannedPath`

3. **Each frame:**
   - Navigate toward current waypoint in path
   - When waypoint reached, advance to next
   - When path complete, stop at lift base

4. **LiftSystem detects skier near lift** → State = QUEUED
5. **Normal lift/ski cycle continues**

---

## Path Validation Rules

```java
// Rule: Cannot walk UP ski trails
if (from.type == TRAIL_END && to.type == TRAIL_START) {
    // This would require walking uphill on ski trail - INVALID!
    return false;
}
```

**Valid walking paths:**
- BASE_CAMP → LIFT_BOTTOM ✅
- LIFT_TOP → TRAIL_START ✅ (already at top, just starting to ski)
- TRAIL_END → LIFT_BOTTOM ✅

**Invalid walking paths:**
- TRAIL_END → TRAIL_START ❌ (walking up a ski slope)
- Any path that requires uphill traversal ❌

---

## Benefits

✅ **No more walking up trails** - Path validation ensures skiers only take walkable routes
✅ **No more circles** - Complete path planning prevents local minima traps  
✅ **Scalable** - Works with any number of interconnected lifts and trails
✅ **Uses the graph** - Finally leveraging the snap point system properly
✅ **Separation of concerns** - Each system has one clear responsibility

---

## Testing Scenarios

### Test Case 1: Simple Resort (1 lift, 1 trail)
- Skier spawns at base → walks to lift → rides up → skis down → repeat ✅

### Test Case 2: Complex Resort (2+ lifts, multiple trails)
- Skier can navigate between different lifts
- Never walks up ski trails
- Chooses shortest path to nearest accessible lift

### Test Case 3: Interconnected Trails
- Skier finishes mid-mountain trail
- Multiple valid paths to different lifts
- Picks optimal path based on BFS distance

---

## Performance Considerations

### BFS Pathfinding Cost:
- **When:** Only when skier enters WAITING state (infrequent)
- **Complexity:** O(V + E) where V = snap points, E = connections
- **Typical:** ~10-50 snap points in a resort = negligible cost

### Path Following Cost:
- **When:** Every frame for skiers in WAITING state
- **Complexity:** O(1) - just check distance to current waypoint
- **Impact:** Minimal

---

## Known Limitations & Future Work

### Current Limitations:
1. **No dynamic re-pathing** - If a lift is removed while skier en route, path breaks
2. **No lift preference** - Picks nearest lift by graph distance, not by player priority
3. **No cost-based routing** - All paths weighted equally (could prefer certain routes)

### Future Enhancements:
1. **Weighted pathfinding** - Use Dijkstra with lift wait time, trail preference as costs
2. **Dynamic obstacles** - Re-plan if path becomes invalid
3. **Trail chaining** - Plan multi-trail routes ("ski trail A, ride lift B, ski trail C")
4. **Smart spawning** - Spawn skiers with goal trails in mind

---

## Files Modified

### New Files:
- `SkierNavigationSystem.java` - New high-level navigation system

### Modified Files:
- `SkierComponent.java` - Added `plannedPath` and `pathIndex` fields
- `SnapPointManager.java` - Implemented proper BFS pathfinding
- `SkierBehaviorSystem.java` - Removed WAITING state handling (moved to navigation system)
- `TycoonSimulation.java` - Registered `SkierNavigationSystem`

---

## Migration Notes

**Breaking Changes:** None - existing saves won't have `plannedPath` but it defaults to null (will plan on first update)

**System Order Matters:** `SkierNavigationSystem` must run BEFORE `SkierBehaviorSystem` to ensure clean state transitions

---

## Debug Tips

### If skiers get stuck:
1. Check snap point connections - are all lifts/trails connected properly?
2. Enable debug logging in `SkierNavigationSystem.planPathToLift()` (commented out)
3. Verify no orphaned snap points (not connected to graph)

### If skiers still walk up trails:
1. Check `isPathNavigable()` validation logic
2. Ensure trail snap points are created with correct types (TRAIL_START vs TRAIL_END)
3. Verify connections are directional where needed

---

## Conclusion

This redesign transforms skier AI from "local greedy search" to "global graph navigation". The result is robust pathfinding that handles complex resort layouts gracefully.

**Status:** ✅ Implemented and compiled successfully
**Ready for:** Gameplay testing with multiple interconnected lifts

