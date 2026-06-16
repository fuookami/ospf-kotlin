# demo4 — Crew Scheduling with Gantt Scheduling Framework

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`demo4` demonstrates **crew scheduling** using the `gantt_scheduling` framework. It models flight recovery scenarios where crew members must be assigned to flight tasks while respecting duty time limits, connection rules, and fleet balance constraints. The demo also includes a generic quantity sample showing how to use framework types for various scheduling dimensions.

## Scope

- Model crew members (pilots, crew) with ranks and schedules.
- Define flight tasks, legs, and recovery scenarios.
- Generate crew bunches (feasible duty sequences).
- Compile bunches into flight-linked schedules.
- Apply fleet balance and capacity constraints.
- Use generic quantities for time, cost, resource capacity, and switches.

## Module Structure

| Domain Context | Responsibility |
| --- | --- |
| `crew` | Crew domain: pilots, crew members, ranks, schedules, transit times |
| `task` | Flight tasks: legs, aircraft, airports, passengers, recovery, maintenance |
| `rule` | Scheduling rules: links, locks, flow control, restrictions, cost calculation |
| `cargo` | Cargo domain: aggregation and context |
| `passenger` | Passenger management: cancellations, changes, capacity constraints |
| `bunch_generation` | Generate feasible crew bunches from flight graph |
| `bunch_compilation` | Compile bunches: fleet balance, flight links, flight capacity |
| `bunch_selection` | Branch-and-price algorithm for bunch selection |
| `infrastructure` | Solver, DTOs (Input/Output), semantic parameters |

## Architecture

```
demo4/
  Application.kt              -- Entry point and generic quantity sample
  infrastructure/
    Solver.kt                  -- Solver configuration
    SemanticParameter.kt       -- Semantic parameter definitions
    Instant.kt                 -- Time utilities
    dto/
      Input.kt                 -- Input DTO definitions
      Output.kt                -- Output DTO definitions
  domain/
    crew/
      CrewContext.kt           -- Crew context
      Aggregation.kt           -- Crew aggregation
      model/                   -- Crew, Pilot, CrewMan, ranks, schedules
    task/
      FlightTaskContext.kt     -- Flight task context
      Aggregation.kt           -- Task aggregation
      model/                   -- FlightLeg, Aircraft, Airport, Passenger, etc.
    rule/
      RuleContext.kt           -- Rule context
      Aggregation.kt           -- Rule aggregation
      model/                   -- Link, Lock, FlowControl, Restriction
      service/                 -- CostCalculator, FeasibilityJudger, etc.
    cargo/
      CargoContext.kt          -- Cargo context
      Aggregation.kt           -- Cargo aggregation
    passenger/
      PassengerContext.kt      -- Passenger context
      Aggregation.kt           -- Passenger aggregation
      model/                   -- Passenger, PassengerAmount, etc.
      service/limits/          -- Cancellation, change, capacity constraints
    bunch_generation/
      Aggregation.kt           -- Bunch generation aggregation
      model/Graph.kt           -- Flight graph
      service/                 -- BunchGenerator, GraphGenerator, etc.
    bunch_compilation/
      BunchCompilationContext.kt -- Bunch compilation context
      Aggregation.kt           -- Compilation aggregation
      model/                   -- FlightLink, FleetBalance, FlightCapacity
      service/limits/          -- FlightLinkLimit, FleetBalanceLimit
    bunch_selection/
      BunchSelectionContext.kt -- Bunch selection context
      service/BranchAndPriceAlgorithm.kt -- Branch-and-price solver
```

## Generic Quantity Sample

The `Demo4GenericQuantitySample` object demonstrates how to use framework generic types:

| Type | Description |
| --- | --- |
| `TimeRange` | Time interval with start/end instants |
| `Cost<Flt64>` | Cost quantity |
| `MaterialDemand<Flt64>` | Material demand quantity |
| `ResourceCapacity<Flt64>` | Resource capacity quantity |
| `TaskTime<Flt64>` | Task time quantity |
| `Switch<Flt64>` | Switch/change quantity |
| `Makespan<Flt64>` | Makespan quantity |

## Usage

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo4.Application

suspend fun main() {
    val app = Application()
    // ... configure input with crew, flights, rules
    // val result = app(input)
}
```

For the generic quantity sample:

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo4.Demo4GenericQuantitySample

fun main() {
    Demo4GenericQuantitySample.run()
}
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
