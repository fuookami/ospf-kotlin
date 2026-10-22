# demo2 — Aircraft Cargo Stowage Optimization

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`demo2` is a framework example for **aircraft cargo stowage optimization**. It determines how to load cargo items into an aircraft while satisfying airworthiness, structural, and operational constraints. The three public application modes support direct MILP and adaptive Benders decomposition, and return stable response and optional render DTOs.

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

## Solve Modes and Responses

`FullLoadAlgorithm`, `PredistributionApplication`, and `WeightRecommendationApplication` accept a `RequestDTO` and return `Pair<ResponseDTO, RenderDTO?>`.

| Request configuration | Effective path | Response behavior |
| --- | --- | --- |
| `preferBenders = false` | Direct MILP | Successful responses contain `solver_path=milp_direct`. |
| `preferBenders = true` and the binary-variable threshold is met | Gurobi Benders | Successful responses contain `solver_path=benders`. |
| Benders fails and `bendersFallbackToMilp = true` | Direct MILP fallback | Successful responses contain `solver_path=milp_fallback_after_benders`. |
| Benders fails and fallback is disabled | No incumbent | The response preserves `NotConverged`, proven `Infeasible`, or an input/backend `Error`. |

`ResponseDTO.status` distinguishes `Optimal`, `Feasible`, `Infeasible`, `NotConverged` and `Error`. Only unknown aircraft types return `UnsupportedAircraft`. All four supported types and all three loading modes support MILP and linear Benders; real solves require a working Gurobi installation and license. `notes` and `diagnostics` preserve solver-path details.

## Mode Boundaries

| Mode | Additional behavior |
| --- | --- |
| `FullLoad` | Registers the standard stowage, airworthiness, MAC, soft-security, express, and loading-effectiveness chains. High-priority cargo is required to ship. |
| `Predistribution` | Adds redundancy and loading-order behavior. Must-ship and source-early constraints are not registered. |
| `WeightRecommendation` | Adds recommended-weight equalization and payload maximization. High-priority cargo is required to ship; MAC optimization and soft-security objectives are excluded. |

Domain extensions belong in a context, aggregation, model component, pipeline, or limit. Applications only select the solve path, map structured failures, and assemble `ResponseDTO` and optional `RenderDTO`.

## Request controls and validation profiles

`RequestDTO` exposes the payload ratio, adjacent-gap, cumulative-load, envelope, target-moment, longitudinal-deviation, and lateral-imbalance settings. These values are registered as hard model constraints for every supported mode; they are not diagnostic-only fields. The weight-recommendation settings `balancePriority` and `payloadPriority` are converted into `Parameter` and change the recommendation objective coefficients.

The ordinary `demo2-only` Maven profile compiles the Demo2 application and migration tests without a commercial solver dependency. The licensed application E2E is isolated in `demo2-gurobi-e2e`; it compiles only `Demo2ApplicationE2EIntegration` and requires a working Gurobi installation and license. Both the direct MILP and Benders paths use the same mode-specific context and pipeline registration boundaries.

The reduced `RequestDTO` does not carry horizontal-stabilizer lookup points or a maximum-trim input. Its MAC initializer therefore keeps the stabilizer collection empty, matching the Rust example; no invented trim constant is presented as a hard constraint.

The airworthiness initializer creates the default linear-density and surface-density zones, request envelope, target-moment/lateral-imbalance bounds, and wide-body CLIM points. `MaxUnsymmetricalLinearDensity` and horizontal-stabilizer limits are only registered when explicit domain data is supplied; they are not synthesized from the reduced DTO. The default longitudinal envelope is registered once through `EnvelopeLimit`, while the target-moment bound is provided by its dedicated limit.

## Usage

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo2.FullLoadAlgorithm
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.RequestDTO

suspend fun main() {
    val (response, render) = FullLoadAlgorithm(
        request = RequestDTO.sample(),
        withRender = true
    )
    if (response.succeed) {
        println(response.assignments)
        println(render?.positions)
    } else {
        println("${response.status}: ${response.notes}")
    }
}
```

## Limit alignment and capacity decision

The four Rust-aligned limits are registered in their domain pipelines:

| Limit | Pipeline | Semantics |
| --- | --- | --- |
| `AdjacentGapLimit` | `airworthiness_security` | Adds two hard inequalities for the absolute load gap of each adjacent position pair. |
| `MustShipLimit` | `express_effectiveness` | Adds one hard equality requiring each selected cargo item to be assigned exactly once; enabled only for `FullLoad` and `WeightRecommendation`. |
| `PriorityOrderLimit` | `loading_effectiveness` | Adds a big-M hard inequality for every strictly higher-priority cargo pair, where big-M is the position count. |
| `SourceEarlyLimit` | `loading_effectiveness` | Adds a hard lower bound requiring multi-cargo sources to reach an early position; disabled for `Predistribution`. |

`LoadWeightLimit` remains registered by the `stowage` pipeline and owns the per-position maximum load-weight constraint. No separate airworthiness `CapacityLimit` is added, avoiding duplicate responsibility. The mode selectors and derived data were checked against `E:/workspace/ospf/ospf-rust/ospf-rust-example/src/framework/demo2`: must-ship uses priority `>= 8`, cargoes are grouped by source, `earlyEnd` is `(positionCount - 1) / 2`, and big-M is the position count.

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pdemo2-only test
```

## Explicit Geometry Contract (Breaking Change)

Requests must declare `lengthUnit`, `weightUnit`, `spaces`, `layoutGroups` and
`positionConflicts` (explicit empty collections are valid). Each position must include
`maxWeight`, `maxLoadCount`, `spaceId`, `geometry {front, back, left, right}` and a unique `loadingOrder` in 0..255.
No layout is inferred from list order, fixed lateral width or a default area. Area is
computed from declared rectangle bounds. Explicit moment arms remain independent
business inputs, normalized from the request length unit; moment constraints use the
request length/weight unit product. Geometric centers do not silently replace arms.

B737/B757 use `bpp1d`; B767/B747 use `bpp2d`. Unknown aircraft types are rejected.
A physical-space ID isolates decks; geometric overlap cannot be cancelled by an empty
business conflict list. Layout groups govern exactly-one regional/global choices;
without groups, non-conflicting regions can mix. Cargo declares `deckLocation` (main by
default) and may further restrict `allowedPositions`. Lower-deck cargo must explicitly
declare `LowForward` or `LowAft`; no deck is inferred from names or list order.
`loadedItems` fixes cargo assignments; `ala` / `alw` are advisory soft targets, not
additional existing cargo.
Full-load payload sums actual assignments rather than all offered optional cargo;
fixed cargo is counted once as well.
Predistribution conserves the planned payload through an equality over actual and
additional position weights, rather than an unrelated constant payload expression.

`StowageContext` adapts aviation inputs into shared sparse contexts. The three public
entries delegate to a common service; all aviation contexts are rebuilt on fallback.
The old demo-specific Benders entry is a compatibility adapter to the shared executor,
not an independent iteration algorithm. Response fields `frameworkPath`,
`selectedLayouts`, `occupiedPositions` and the actual solver `status` expose the path.
An incomplete feasible solve is not labeled optimal.

Every position also requires an explicit `partitionId`, independent of its physical space/deck.
Set `SolvePolicy(partitionColumnGeneration = true)` to use complete partition configurations in
all three loading modes and all four aircraft types. Direct MILP, partition CG and linear Benders
are independent strategies; simultaneously enabling `preferBenders` is rejected, not treated as a joint algorithm.
All aviation rows/objectives participate before pricing, including cross-partition conflicts,
shared layouts and global MAC/CLIM constraints. Estimated/recommended weights keep their continuous/integer
types and explicit partition ownership. Final resource completion stays inside generated assignment/occupancy
profiles and materializes complete columns before the integer master. Without a global integer proof,
success returns `Feasible` with `solver_path=partition_column_generation`, column/iteration counts and root LP convergence.

The example supplies simplified aircraft/airworthiness data, not an airworthiness-
certified aircraft database. Geometry support does not certify a real loading plan.

For isolated verification without unrelated example modules:

```powershell
mvn -B -ntp -pl ospf-kotlin-example -am test -Pdemo2-only '-Dospf.demo2.isolated=true' -T 0.75C *> "$env:TEMP/demo2-tests.log"
mvn -B -ntp -pl ospf-kotlin-example -am test -Pdemo2-gurobi-e2e '-Dospf.demo2.isolated=true' -T 0.75C *> "$env:TEMP/demo2-gurobi.log"
```
