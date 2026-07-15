# demo2 — Aircraft Cargo Stowage Optimization

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`demo2` is a full-scale production example for **aircraft cargo stowage optimization**. It determines how to load cargo items into an aircraft while satisfying airworthiness, structural, and operational constraints. The model supports multiple solve paths including direct MILP and Benders decomposition.

## Scope

- Model aircraft physical properties (fuselage, deck, fuel, hatch doors).
- Define cargo items with positions, loads, and appointments.
- Enforce airworthiness constraints (CLIM, envelope, longitudinal/lateral balance).
- Optimize MAC (Mean Aerodynamic Chord) for center-of-gravity control.
- Apply soft security constraints (ballast, empty loading).
- Maximize payload while respecting structural limits.
- Support express cargo priority and loading order optimization.

## Module Structure

| Domain Context | Responsibility |
| --- | --- |
| `aircraft` | Aircraft model: fuselage, deck, fuel, hatch doors, loading order |
| `stowage` | Cargo stowage: items, loads, positions, appointments |
| `mac` | Mean Aerodynamic Chord calculations |
| `airworthiness_security` | Structural limits: CLIM, envelope, zone weights, cumulative loads |
| `soft_security` | Ballast and empty loading constraints |
| `mac_optimization` | Center-of-gravity optimization (lateral/longitudinal balance) |
| `payload_maximization` | Maximize cargo payload |
| `express_effectiveness` | Express cargo priority ordering |
| `loading_effectiveness` | Loading order, trailer management, sequential loading |
| `recommended_weight_equalization` | Weight equalization and priority appointments |
| `redundancy` | Redundancy constraints and experimental balance |
| `infrastructure` | Solver configuration, DTOs, Benders strategy, diagnostics |

## Architecture

Each domain context follows the DDD pattern:

```
domain/<context>/
  <Context>Context.kt     -- Context class: init(), register(), construct(), analyze()
  Aggregation.kt          -- Aggregation root: holds domain model state
  model/                  -- Domain entities and value objects
  service/
    AggregationInitializer.kt  -- Initialize aggregation from input
    PipelineListGenerator.kt   -- Register constraints into model
    limits/                    -- Constraint definitions (business rules)
    SolutionAnalyzer.kt        -- Extract solution for this context
```

## Application Entry Points

| File | Description |
| --- | --- |
| `FullLoadApplication.kt` | Full load optimization |
| `LoadingOrderApplication.kt` | Loading order generation |
| `PredistributionApplication.kt` | Predistribution optimization |
| `WeightRecommendationApplication.kt` | Weight recommendation |

## Usage

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo2.FullLoadApplication

suspend fun main() {
    val app = FullLoadApplication()
    val input = RequestDTO(/* aircraft config, cargo items */)
    val result = app(input)
    when (result) {
        is Ok -> println("Stowage plan: ${result.value}")
        is Failed -> println("Failed: ${result.error}")
        is Fatal -> println("Fatal: ${result.errors}")
    }
}
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
