# Rule

:us: English | :cn: [简体中文](README_ch.md)

Bounded context responsible for defining and evaluating operational rules, restrictions, and flow controls in the flight recovery scheduling system. It provides the rule-checking infrastructure used by other domains (especially bunch_generation) to determine feasibility and cost of recovery plans.

## Responsibilities

- Define flight links (connecting, stopover, connection-time-ignoring) between consecutive flight tasks.
- Model locks that prevent modification of specific tasks.
- Define flow control rules at airports (departure/arrival capacity limits).
- Define restrictions (relation-based and general) with severity levels (weak, violable-strong, strong).
- Calculate costs, connection times, minimum departure times, and feasibility for task sequences.

## Key Classes

| Class | Description |
|---|---|
| `RuleContext` | Entry point for rule domain operations. |
| `Aggregation` | Aggregation of rule domain objects. |
| `Link` | Sealed class: `ConnectingLink`, `StopoverLink`, `ConnectionTimeIgnoringLink`. |
| `LinkMap` | Provides lookup of all link types by predecessor/successor task. |
| `Lock` | Prevents flight task modifications. |
| `FlowControl` | Airport capacity limit rule for a given scene and time range. |
| `Flow` | A flow resource representing a set of flow control rules at an airport. |
| `FlowControlScene` | Enum: `Departure`, `Arrival`, `DepartureArrival`, `Stay`. |
| `Restriction` | Sealed interface: `RelationRestriction`, `GeneralRestriction`. |
| `RestrictionType` | Enum: `Weak`, `ViolableStrong`, `Strong`. |
| `CostCalculator` | Calculates costs for task sequences. |
| `ConnectionTimeCalculator` | Calculates connection times between tasks. |
| `MinimumDepartureTimeCalculator` | Calculates minimum departure times. |
| `FeasibilityJudger` | Evaluates feasibility of task sequences. |

## Dependencies

- **task** — provides `FlightTask`, `FlightTaskBunch`, `Aircraft`, `Airport`, `FlightType`.
- **framework (gantt_scheduling)** — provides `ConnectionResource`, `AbstractResourceCapacity`.
