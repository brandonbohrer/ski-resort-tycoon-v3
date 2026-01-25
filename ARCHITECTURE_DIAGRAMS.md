# Skier AI System Architecture

## State Machine Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    SKIER LIFECYCLE                          │
└─────────────────────────────────────────────────────────────┘

    SPAWN
      ↓
┌──────────────────────────────────────────┐
│  WAITING (Find & Navigate to Lift)      │  ← SkierNavigationSystem
│  • Plan path using snap point graph     │
│  • Walk toward waypoints                │
│  • Validate path (no walking up trails) │
└──────────┬───────────────────────────────┘
           │ (Arrives at lift base)
           ↓
┌──────────────────────────────────────────┐
│  QUEUED (Wait in line)                   │  ← LiftSystem
│  • Stand still in queue                  │
│  • Wait for boarding timer               │
└──────────┬───────────────────────────────┘
           │ (Boards lift)
           ↓
┌──────────────────────────────────────────┐
│  RIDING_LIFT (Transport uphill)          │  ← LiftSystem
│  • Follow pylon chain                    │
│  • Move at lift speed                    │
└──────────┬───────────────────────────────┘
           │ (Reaches lift top)
           ↓
┌──────────────────────────────────────────┐
│  SKIING (Ski down trail)                 │  ← SkierBehaviorSystem
│  • Carving turns & realistic physics     │
│  • Trail difficulty matching             │
│  • Satisfaction tracking                 │
└──────────┬───────────────────────────────┘
           │ (Reaches base)
           ↓
┌──────────────────────────────────────────┐
│  FINISHED (Ready for despawn)            │  ← SkierSpawnerSystem
│  • Cleanup & remove entity               │
└──────────────────────────────────────────┘
```

---

## Snap Point Graph Structure

```
┌─────────────────────────────────────────────────────────┐
│                   SNAP POINT NETWORK                    │
└─────────────────────────────────────────────────────────┘

      BASE_CAMP
         │
         │ (bidirectional)
         ↓
    LIFT_BOTTOM (Lift A)
         │
         │ (lift chain)
         ↓
     LIFT_TOP (Lift A)
         │
         │ (bidirectional)
         ↓
    TRAIL_START ─────→ TRAIL_END
    (Ski direction)        │
                           │
                           ↓
                      LIFT_BOTTOM (Lift A)
                           │
                           ↓ (LOOP BACK TO TOP)

Multiple lifts/trails create interconnected graph:

    BASE_CAMP ← → LIFT_BOTTOM (A) ← → LIFT_BOTTOM (B)
                       ↑                    ↑
                  TRAIL_END (1)        TRAIL_END (2)
                       ↑                    ↑
                  TRAIL_START (1)     TRAIL_START (2)
                       ↑                    ↑
                  LIFT_TOP (A)        LIFT_TOP (B)
```

---

## Pathfinding Example

### Scenario: Skier at mid-mountain needs to return to base

```
Initial State:
  Skier Position: (x=120, z=150) near TRAIL_END (Trail 2)
  Goal: Reach any LIFT_BOTTOM

Step 1: Find Nearest Snap Point
  → findNearestSnapPoint(120, 150, radius=50)
  → Returns: TRAIL_END (Trail 2)

Step 2: Get All Lift Bases
  → getSnapPointsByType(LIFT_BOTTOM)
  → Returns: [LIFT_BOTTOM (A), LIFT_BOTTOM (B)]

Step 3: BFS Pathfinding to Each
  Path to Lift A:
    TRAIL_END → LIFT_BOTTOM (A)
    Length: 2 nodes ✅
  
  Path to Lift B:
    TRAIL_END → BASE_CAMP → LIFT_BOTTOM (B)
    Length: 3 nodes ✅

Step 4: Validate Paths
  ✅ Path to Lift A: Valid (no uphill trail traversal)
  ✅ Path to Lift B: Valid (walks through base camp)

Step 5: Choose Shortest
  → Pick Path to Lift A (2 nodes)
  → Store in skier.plannedPath = [TRAIL_END_ID, LIFT_BOTTOM_A_ID]

Step 6: Execute Navigation
  Frame 1-50:  Walk toward TRAIL_END waypoint
  Frame 51:    Reach TRAIL_END, advance to next waypoint
  Frame 52-100: Walk toward LIFT_BOTTOM (A)
  Frame 101:   Reach LIFT_BOTTOM (A), path complete
               → LiftSystem detects skier, transitions to QUEUED
```

---

## Path Validation Logic

```
Function: isPathNavigable(path, isOnFoot)

For each segment in path:
  from_type = path[i].type
  to_type = path[i+1].type
  
  IF isOnFoot AND from_type == TRAIL_END AND to_type == TRAIL_START:
    ❌ INVALID - Would require walking UP a ski trail
    
Example Invalid Path:
  [TRAIL_END] → [TRAIL_START] → [LIFT_TOP]
   └─ Walking uphill! ❌

Example Valid Path:
  [TRAIL_END] → [LIFT_BOTTOM] → [BASE_CAMP]
   └─ Walking on flat/downhill ✅
```

---

## System Responsibilities Matrix

| State         | Navigation | Physics | Queuing | Spawning |
|---------------|------------|---------|---------|----------|
| WAITING       | Nav ✅     |         |         |          |
| QUEUED        |            |         | Lift ✅  |          |
| RIDING_LIFT   |            |         | Lift ✅  |          |
| SKIING        |            | Behav ✅ |         |          |
| FINISHED      |            |         |         | Spawn ✅ |

**Legend:**
- Nav = SkierNavigationSystem
- Behav = SkierBehaviorSystem  
- Lift = LiftSystem
- Spawn = SkierSpawnerSystem

---

## Data Flow

```
┌─────────────────────┐
│  SkierComponent     │
│  • state            │
│  • plannedPath ─────┼──► Used by SkierNavigationSystem
│  • pathIndex        │      to track waypoint progress
└─────────────────────┘
         ↑
         │ Read/Write
         ↓
┌─────────────────────┐
│ SkierNavigation     │
│ System              │
│  • Plans paths      │
│  • Updates velocity │
└────────┬────────────┘
         │ Queries
         ↓
┌─────────────────────┐
│ SnapPointManager    │
│  • Graph storage    │
│  • BFS pathfinding  │──┐
└─────────────────────┘  │
                         │
         ┌───────────────┘
         ↓
┌─────────────────────┐
│  Snap Point Graph   │
│  • Nodes (points)   │
│  • Edges (connects) │
└─────────────────────┘
```

---

## Performance Profile

### Pathfinding Cost per Skier:

```
Event: Skier enters WAITING state

1. Find nearest snap point: O(N) where N = total snap points
   Typical: 20-50 points → ~50 checks

2. Get lift bases: O(N)
   Typical: Filter 20-50 points → ~50 checks

3. BFS for each lift: O(V + E) where V = vertices, E = edges
   Typical: 20 points, 30 connections → ~50 operations per lift
   
4. Path validation: O(P) where P = path length
   Typical: 2-5 hops → 5 checks

Total: ~200-300 operations per skier per planning cycle
Frequency: Once every 30-60 seconds (when they finish skiing)

With 75 skiers:
  Worst case: All plan at once = 22,500 ops
  Typical: Staggered = ~300 ops/frame
  Impact: NEGLIGIBLE on modern CPU
```

### Navigation Cost per Frame:

```
For each skier in WAITING state:
  1. Check distance to current waypoint: 1 sqrt operation
  2. Calculate velocity: 2 float divisions
  
Total: ~3-5 operations per skier per frame
With 20 skiers in WAITING: ~100 ops/frame
Impact: NEGLIGIBLE
```

---

## Debugging Guide

### Enable Debug Logging:

In `SkierNavigationSystem.java`, uncomment:
```java
// System.out.println("Skier planned path to lift: " + bestPath.size() + " waypoints");
// System.out.println("Skier could not find valid path to any lift");
```

### Common Issues:

**1. Skiers don't move in WAITING state**
   - Check: Are snap points registered properly?
   - Check: Are snap points connected (bidirectional)?
   - Debug: Print `snapPointManager.getAllSnapPoints().size()`

**2. Skiers walk up trails**
   - Check: Are TRAIL_START/TRAIL_END types correct?
   - Check: Is `isPathNavigable()` being called?
   - Debug: Print path before validation

**3. Skiers walk in circles**
   - Check: Is there a valid path to lift base?
   - Check: Are all snap points in connected component?
   - Debug: Print BFS result for disconnected nodes

**4. Skiers ignore lifts**
   - Check: Are LIFT_BOTTOM snap points registered?
   - Check: Is `SkierNavigationSystem` added to simulation?
   - Debug: Print system update order
```

