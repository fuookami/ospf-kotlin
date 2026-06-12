# gantt-scheduling-domain-bunch-generation-context

:us: English | :cn: [简体中文](README_ch.md)

Pricing problem for column generation — generates new bunches (routes) with negative reduced cost using label-setting shortest path with resource constraints.

## Key Types

| Type | Description |
|------|-------------|
| `Node` | Sealed class for graph vertices: `RootNode`, `EndNode`, `TaskNode` |
| `Edge` | Directed edge connecting nodes in the graph |
| `Graph` | Directed graph structure for path finding |
| `Label` | Label for dynamic programming / shortest path with resource tracking |
| `SlotBasedBunchGenerator` | Generates bunches based on time slot availability |
| `PlannedTaskBunchGenerator` | Generates bunches from planned tasks |
| `UnplannedTaskBunchGenerator` | Generates bunches from unplanned tasks |

## Dependencies

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`
