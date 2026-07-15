# Passenger

:us: English | :cn: [简体中文](README_ch.md)

Bounded context responsible for passenger domain operations in the flight recovery scheduling system. It models passengers with multi-leg itineraries, tracks cancellations, class changes, and flight changes, and registers passenger-related constraints and objectives into the column generation model.

## Responsibilities

- Model passengers with amounts, routes, and per-leg class assignments.
- Track passenger cancellations as decision variables.
- Model class-change and flight-change variables for disrupted passengers.
- Compute per-flight per-class passenger amount expressions.
- Register cancellation minimization, class-change minimization, flight-change minimization, route-cancel constraints, and flight-capacity constraints.
- Generate column generation pipelines for passenger constraints.

## Key Classes

| Class | Description |
|---|---|
| `PassengerContext` | Entry point managing aggregation and pipeline registration. |
| `Aggregation` | Combines `PassengerCancel`, `PassengerChange`, and `PassengerAmount`. |
| `Passenger` | A passenger with amount and multi-leg flight list. |
| `FlightPassenger` | Links a passenger to a specific flight with optional previous leg. |
| `PassengerCancel` | Tracks cancellation decision variables. |
| `PassengerChange` | Tracks class-change and flight-change decision variables. |
| `PassengerAmount` | Computes passenger amount expressions per flight and class. |
| `PipelineListGenerator` | Generates CG pipelines for passenger constraints. |

## Dependencies

- **task** — provides `FlightTask`, `FlightTaskBunch`, `PassengerClass`.
- **bunch_compilation** — provides `FlightCapacity`, `TaskTime`.
