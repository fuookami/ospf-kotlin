# Task

:us: English | :cn: [简体中文](README_ch.md)

Bounded context responsible for defining the core flight task domain model in the flight recovery scheduling system. It provides the fundamental entities—airports, aircraft, flight legs, maintenance, AOG (aircraft on ground), transfers, and flight task bunches—that all other domains depend on.

## Responsibilities

- Define airport entities with ICAO codes, types, and transfer times.
- Model aircraft with register numbers, minor types, capacity, and usability tracking.
- Define flight task hierarchy: `FlightTask` (abstract) with concrete types `FlightLeg`, `Maintenance`, `AOG`, `Transfer`.
- Model flight task plans with scheduled/estimated/actual times and recovery policies.
- Define `FlightTaskBunch` — an ordered sequence of tasks assigned to a single aircraft.
- Provide shadow price types for column generation.
- Track flight hours, flight cycles, and aircraft change detection.

## Key Classes

| Class | Description |
|---|---|
| `FlightTaskContext` | Entry point for flight task domain operations. |
| `Aggregation` | Combines airports, aircraft, legs, maintenances, AOGs, transfers, and origin bunches. |
| `Airport` | Airport identified by ICAO code with type and transfer times. |
| `Aircraft` | Aircraft identified by register number with minor type and capacity. |
| `AircraftUsability` | Tracks aircraft location, enabled time, and flight cycle periods. |
| `FlightTask` | Abstract base for all flight tasks with plan, recovery, and delay tracking. |
| `FlightLeg` | A concrete flight leg with optional recovery aircraft/time. |
| `FlightLegPlan` | Flight leg plan with scheduled/estimated/actual times. |
| `FlightTaskBunch` | Ordered sequence of tasks assigned to a single aircraft. |
| `FlightTaskAssignment` | Recovery policy specifying optional aircraft, time, and route changes. |
| `ShadowPriceMap` | Map of shadow prices for column generation pricing. |

## Dependencies

- **infrastructure** — provides `ICAO`, `AircraftRegisterNumber`, `AircraftMinorType`, `AircraftCapacity`, `PassengerClass`.
- **framework (gantt_scheduling)** — provides base `Executor`, `AbstractTask`, `AbstractTaskBunch`, `TimeRange`, `TimeWindow`.
