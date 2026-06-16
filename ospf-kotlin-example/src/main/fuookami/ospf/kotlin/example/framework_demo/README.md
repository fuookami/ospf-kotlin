# framework_demo

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`framework_demo` contains framework-oriented example applications that demonstrate DDD (Domain-Driven Design) style optimization modeling with OSPF Kotlin. Each demo organizes its domain logic into **contexts**, where each context owns an **aggregation** (domain model), **service** layer (business limits/constraints), and registers its variables and constraints into a shared `LinearMetaModel`.

## Scope

| Demo | Description | Details |
| --- | --- | --- |
| [`demo1`](demo1/README.md) | Network routing with bandwidth constraints | Shortest Service Path (SSP) using `route_context` and `bandwidth_context` |
| [`demo2`](demo2/README.md) | Aircraft cargo stowage optimization | Full-scale production example with 11 domain contexts, supports Benders decomposition |
| [`demo3`](demo3/README.md) | 1D Cutting Stock Problem (CSP1D) | Column generation with SCIP solver |
| [`demo4`](demo4/README.md) | Crew scheduling | Generic quantity usage across `gantt_scheduling` framework types |

## Module Structure

| Demo | Description | Domain Contexts |
| --- | --- | --- |
| `demo1` | Network routing with bandwidth constraints | `route_context` (route assignment), `bandwidth_context` (bandwidth allocation) |
| `demo2` | Aircraft cargo stowage optimization | `aircraft` (aircraft model, deck, fuel), `stowage` (cargo, load, position), `mac` (mean aerodynamic chord), `airworthiness_security` (structural limits), `soft_security` (ballast, empty loading), `mac_optimization` (CG optimization), `express_effectiveness` (express cargo priority), `loading_effectiveness` (loading order, trailer) |
| `demo3` | 1D Cutting Stock Problem (column generation) | `material` (product, material width range), `cutting_plan_generation` (pricing, initial plan generation) |
| `demo4` | Gantt scheduling generic quantity sample | `task` (task time, switch), `produce` (production, consumption), `resource` (capacity, usage), `bunch_compilation` (iteration, solution summary) |

## Architecture Pattern

Each domain context follows this structure:

```
context/
  Context.kt              -- Context class: init(), register(), construct(), analyze()
  Aggregation.kt          -- Aggregation root: holds domain model state
  model/                  -- Domain model classes (entities, value objects)
  service/
    AggregationInitializer.kt  -- Initializes the aggregation from input
    limits/                    -- Constraint definitions (business rules)
```

The typical flow is:

1. **init** -- Contexts are initialized from input DTOs, each context's aggregation is populated.
2. **register** -- Each context registers its variables and intermediate symbols into the shared `LinearMetaModel`.
3. **construct** -- Each context adds its constraints (limits) and objective contributions to the model.
4. **solve** -- The model is solved via SCIP or column generation.
5. **analyze** -- Each context extracts its portion of the solution.

## Usage

The entry point for each demo is an `Application` class (or equivalent) with a suspending `invoke()` operator:

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo1.SSP

suspend fun main() {
    val ssp = SSP()
    val input = Input(/* ... */)
    val result = ssp(input)
    when (result) {
        is Ok -> println("Solution: ${result.value}")
        is Failed -> println("Failed: ${result.error}")
        is Fatal -> println("Fatal: ${result.errors}")
    }
}
```

For demo3 (CSP1D column generation):

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo3.CSP

suspend fun main() {
    val csp = CSP()
    csp()
}
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
