# Bunch Compilation

:us: English | :cn: [简体中文](README_ch.md)

Bounded context responsible for compiling flight task bunches into the column generation optimization model. It manages the registration of constraints—task time, flow, fleet balance, flight link, and flight capacity—into the linear meta-model, and supports incremental column addition during branch-and-price iterations.

## Responsibilities

- Register task-time, flow, fleet-balance, flight-link, and flight-capacity constraints into the optimization model.
- Manage the `Compilation` (column generation decision variables) and `PipelineList` (constraint generation pipelines).
- Add new columns for bunches discovered during pricing iterations.
- Select free executors based on shadow prices for the branch-and-price algorithm.

## Key Classes

| Class | Description |
|---|---|
| `BunchCompilationContext` | Entry point managing aggregation and pipeline registration. |
| `Aggregation` | Combines `TaskTime`, `Flow`, `FleetBalance`, `FlightLink`, and `FlightCapacity` sub-aggregations. |
| `Compilation` | Type alias for `BunchCompilation` specialized with flight task types. |
| `FlightLink` | Models link expressions tracking connections between consecutive flight tasks. |
| `FleetBalance` | Ensures aircraft distribution across airports matches expected fleet composition. |
| `FlightCapacity` | Tracks passenger and cargo capacity across flight task bunches. |

## Dependencies

- **task** — provides `FlightTask`, `Aircraft`, `FlightTaskBunch`, `FlightTaskAssignment`, `ShadowPriceMap`.
- **rule** — provides `Link`, `LinkMap`, `Flow`.
- **framework (gantt_scheduling)** — provides base `BunchCompilationContext`, `BunchCompilationAggregation`, `BunchSchedulingTaskTime`, `BunchSchedulingConnectionResourceUsage`.
