# Bunch Selection

:us: English | :cn: [简体中文](README_ch.md)

Bounded context responsible for selecting the optimal set of flight task bunches using the branch-and-price algorithm. It orchestrates the master problem solve and pricing subproblem coordination to find the best bunch combination that covers all required tasks at minimum cost.

## Responsibilities

- Solve the master problem (set-covering / set-partitioning formulation) to select bunches.
- Coordinate with bunch_generation to price new bunches with negative reduced cost.
- Coordinate with bunch_compilation to add new columns to the model.
- Branch on fractional solutions to obtain integer-feasible schedules.

## Key Classes

| Class | Description |
|---|---|
| `BunchSelectionContext` | Entry point for bunch selection operations in the branch-and-price algorithm. |
| `BranchAndPriceAlgorithm` | The branch-and-price solver for the flight recovery scheduling problem. |

## Dependencies

- **bunch_compilation** — registers constraints and adds columns.
- **bunch_generation** — generates new bunches via pricing.
- **task** — provides `FlightTaskBunch`, `ShadowPriceMap`.
