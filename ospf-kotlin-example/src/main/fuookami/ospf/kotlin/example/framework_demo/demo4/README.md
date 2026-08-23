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
| `bunch_generation` | Generate feasible crew bunches from flight graph (pricing sub-problem) |
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
      BunchGenerationContext.kt -- Bunch generation context
      Aggregation.kt           -- Bunch generation aggregation (graphs, reverse, initialBunches)
      model/
        Graph.kt               -- Flight graph (Node, Edge, Graph)
        FlightTaskReverse.kt   -- Reversible task pair management
      service/
        Operator.kt            -- Type aliases (RuleChecker, CostCalculator, etc.)
        FlightTaskFeasibilityJudger.kt -- 10-step feasibility checks
        RouteGraphGenerator.kt -- BFS route graph generation
        FlightTaskBunchGenerator.kt -- Label Setting algorithm
        InitialFlightTaskBunchGenerator.kt -- Initial column generation
        AggregationInitializer.kt -- Initialization orchestration
    bunch_compilation/
      BunchCompilationContext.kt -- Bunch compilation context
      Aggregation.kt           -- Compilation aggregation
      model/                   -- FlightLink, FleetBalance, FlightCapacity
      service/limits/          -- FlightLinkLimit, FleetBalanceLimit
    bunch_selection/
      BunchSelectionContext.kt -- Bunch selection context
      service/BranchAndPriceAlgorithm.kt -- Branch-and-price solver
```

## bunch_generation in detail

`bunch_generation` is the **pricing sub-problem** in branch-and-price. It generates new columns with negative reduced cost.

### Core flow

1. **Initialization** (`AggregationInitializer`)
   - Build `FlightTaskReverse` for reversible task pairs.
   - Build one `RouteGraph` per aircraft with BFS.
   - Generate initial bunches with `InitialFlightTaskBunchGenerator`.

2. **Pricing** (`FlightTaskBunchGenerator`)
   - Run the Label Setting algorithm.
   - Traverse graph nodes in topological order, or use a label queue when order changes are enabled.
   - Accumulate task shadow prices.
   - Apply dominance pruning and the per-node label limit.
   - Return bunches whose reduced cost is negative.

3. **Output**
   - New bunch columns.
   - Route graph and pricing diagnostics for the latest generation round.

### Key concepts

| Concept | Meaning |
| --- | --- |
| `shadow price` | The dual value of a master constraint and its marginal cost. |
| `reduced cost` | Original cost minus the applicable shadow prices; a negative value identifies a useful column. |
| `initial bunch` | An initial feasible column for an aircraft, preserving locked tasks. |
| `generated bunch` | A new column found by the pricing sub-problem. |
| `dominance` | At the same node, retain labels that are no worse in cost, time, and aircraft-change state. |

### Boundary with compilation and selection

| Module | Responsibility | Does not own |
| --- | --- | --- |
| `bunch_generation` | Route graph, initial bunches, pricing, and generation diagnostics. | Master constraints, fleet balance, or solution parsing. |
| `bunch_compilation` | Master constraint registration, fleet balance, and flight links. | Label Setting, route graph construction, or reduced-cost search. |
| `bunch_selection` | Branch-and-price orchestration, shadow-price extraction, and column addition. | The concrete pricing rules. |

The master solver passes shadow prices to `BunchGenerationContext.generateFlightTaskBunch`; the context reuses its static graphs and exposes the latest `routeGraphDiagnostics` and `pricingDiagnostics` to the calling flow.

## Reference Mapping

| Kotlin responsibility | FSRA reference | Rust reference | Intentional difference |
| --- | --- | --- | --- |
| `bunch_generation/BunchGenerationContext.kt`, `AggregationInitializer.kt` | `fsra-domain-bunch-generation-context` aggregation and initialization | `ospf-rust-example/src/framework` bunch-generation context | Kotlin keeps an object context and `Try`/`Ret` error flow; Rust ownership and trait boundaries are not copied literally. |
| `RouteGraphGenerator.kt`, `FlightTaskReverse.kt` | FSRA route graph and reverse-task services | Rust route graph and reverse-task services | Kotlin uses `Graph`, `Node`, and `Edge` domain objects and records route-graph diagnostics. |
| `FlightTaskBunchGenerator.kt` | FSRA pricing and label-setting logic | Rust pricing sub-problem | Kotlin reuses a static graph across pricing rounds and exposes label diagnostics through the context. |
| `bunch_compilation` | FSRA master/bunch compilation context | Rust master model and column compilation | Both own master constraints; pricing and graph traversal remain in `bunch_generation`. |
| `bunch_selection/BranchAndPriceAlgorithm.kt` | FSRA branch-and-price orchestration | Rust branch-and-price orchestration | Kotlin delegates the iteration mechanics to the generic framework algorithm. |

The references are available locally at `E:/workspace/ospf/ospf-rust` and `E:/workspace/fsra-proof`. The FSRA cost-validity guard (`cost == null || !cost.valid`) and label-setting behavior have been checked against `fsra-domain-bunch-generation-context`; Kotlin retains its `Try`/`Ret` boundary and static-graph reuse as documented above.

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
mvn -B -ntp -pl ospf-kotlin-example -Pdemo4-only test
```
