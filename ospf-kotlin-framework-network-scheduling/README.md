# ospf-kotlin-framework-network-scheduling

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-framework-network-scheduling` supports standalone network topology, supply/demand flows, resource paths, network design and explicit-scenario resilience, while retaining the VRPTW (Vehicle Routing Problem with Time Windows) Branch-and-Price entry point. The shared path kernel also serves Gantt network subproblems; Gantt remains responsible for global space-time orchestration.

## Module Structure

```
ospf-kotlin-framework-network-scheduling/
├── network-scheduling-infrastructure/          # Generic graph, arc, capacities, balances, value adapter
├── network-scheduling-domain-path-context/     # Finite-domain, resource-aware path label search
├── network-scheduling-domain-vrp-context/      # VRPTW instance, routes, validators, cost policies
├── network-scheduling-domain-flow-context/     # Generic single-/multi-commodity network flow
├── network-scheduling-domain-design-context/   # Facilities, services, capacity levels and budgets
├── network-scheduling-domain-resilience-context/ # Connectivity witnesses and explicit failure scenarios
├── network-scheduling-domain-route-generation-context/  # ESPPRC pricer, pricing graph, label dominance
├── network-scheduling-domain-route-compilation-context/ # Column pool, CG lifecycle, shadow prices
├── network-scheduling-domain-allocation-context/ # Transport requests, compatibility, quotas and hubs
├── network-scheduling-domain-path-compilation-context/ # Whole-path binary master and lifecycle
├── network-scheduling-domain-structure-context/ # Rooted/Steiner tree and facility ownership
├── network-scheduling-domain-generalized-flow-context/ # Loss/yield and single-input conversion
└── network-scheduling-application/             # B&P algorithm, application service, solve result
```

Dependency graph (arrows mean "depends on"):

```
infrastructure
     ↑
     ├── domain-vrp-context ──→ gantt-scheduling-infrastructure
     ├── domain-flow-context ──→ framework / quantities
     ├── domain-path-context
     ├── domain-design-context ──→ domain-flow-context
     ├── domain-resilience-context ──→ domain-design-context
     ├── domain-route-generation-context ──→ domain-vrp-context / domain-path-context
     └── domain-route-compilation-context ──→ domain-vrp-context
                                      ↑
                                 application
```

`domain-route-generation` and `domain-route-compilation` do not depend on each other; `application` bridges them by passing `PricingDuals` from compilation to generation.


## Scope

This module covers:

1. Generic directed network graphs and flow primitives.
2. Single/multi-commodity network flow modeling.
3. VRPTW instance, routes, validators, and cost policies.
4. ESPPRC exact pricing with label dominance.
5. Incremental column generation with Phase I/II.
6. Best-bound Branch-and-Price orchestration.

Explicit non-goals:

1. Business-specific request DTOs, tenant context, formula languages, and project runtime policies.
2. Solver backend installation and license management.
3. Domain-specific scheduling beyond network-flow problem families.
## Standalone Network And Gantt Subproblems

Network owns topology, flows, resource paths, structural design and explicit-scenario resilience. Gantt owns global tasks, executors, calendars, branch state and scheduling. New Network domains do not depend on Gantt tasks or applications; the existing VRPTW dependency on Gantt `TimeWindow` remains.

- `domain-path-context`: solver-independent finite resource-state search shared by VRPTW and Demo4.
- `domain-flow-context`: immutable `FlowProblem<V>`, per-model `FlowContext<V>`/`FlowAggregation<V>`, conservation, capacity and cost pipelines.
- `domain-design-context`: `NetworkDesignContext<V>` for facilities, services, discrete capacity levels, budgets and flow linkage.
- `domain-resilience-context`: `NetworkResilienceContext<V>` for shared construction, scenario recourse, connectivity witnesses and worst operating cost.
- `application`: `NetworkFlowApplicationService`, `NetworkDesignApplicationService` and `NetworkResilienceApplicationService` orchestrate the existing `ColumnGenerationSolver` without duplicating domain constraints.
- Gantt `gantt-scheduling-domain-network-context` depends on task/bunch contracts and Network path/flow components, not Network application. Use the optional `ospf-kotlin-starter-gantt-network`; the ordinary Gantt starter stays independent.

### Flow And Construction Contracts

Each commodity declares its normalization unit, allowed services, service-specific costs and optional integer measurement step. Integer flow is not unsplittable-order routing. Capacity usage is expressed per normalized commodity unit; incompatible dimensions require explicit `capacityUsage`. Additional `FlowCapacity` rows represent cross-service shared resources or node throughput. Balances are hard by default; `FlowSlack` explicitly bounds unused supply/unmet demand and their per-unit penalty. Results include commodity-service flows, utilization, realized balances and cost breakdown.

`CapacityLevel.capacity` is total built capacity, not an increment. `FlowArc.capacity.upper` is a declared finite envelope that must bound every level. Closed services carry no flow; degraded scenario capacity is the minimum of the built level and the scenario upper bound. Facilities may gate shared processing rows via `NetworkFacility.capacityRows`. No arbitrary big M is used. Multiple capacity levels express expansion; set `required = true` for existing mandatory services.

### Explicit Resilience Contracts

Connectivity supports reachability, edge-disjoint and internally node-disjoint path counts. Auxiliary flows prove structural redundancy, not actual demand capacity. Scenarios explicitly define closed arcs, failed nodes, degraded capacity, balance overrides and optional bounded relaxation. Multiple failures are never exponentially enumerated automatically; failed endpoints do not waive demand. All scenarios share construction but have independent recourse flows. The default minimizes construction plus worst operating cost while meeting all stated scenarios. Guarantees apply only to that scenario set. Repair scheduling remains Gantt's responsibility.

### Standalone And Embedded Use

```kotlin
// Inject the business problem, backend and numeric adapter.
val result = NetworkFlowApplicationService(solver, adapter).solve(problem)

// Embedded registration never starts another solve.
val context = FlowContext(problem, adapter)
val registered = context.register(model)
```

Standalone entries return `NetworkSolveResult`: normal no-incumbent limits and infeasibility retain the solver report with `solution = null`; technical errors use `Ret`. Public values remain `V`/`Quantity<V>`; `Flt64` conversion occurs at registration/extraction boundaries. Extend context expressions, aggregations or pipelines rather than application orchestration.

Runnable native-free examples are in `FlowContextTest` (transportation/transshipment, heterogeneous units, integer steps, soft balances), `NetworkDesignContextTest` (facility/service/expansion/budget enumeration), `NetworkResilienceContextTest` (recourse, disjointness, shortfall), and `NetworkApplicationServiceTest` (standalone outcomes). Real backend tests require explicit profiles; a skipped test is not a pass.

See [migration notes](MIGRATION.md).

## Public API

### Infrastructure

| Type | Purpose |
|------|---------|
| `NetworkNodeId` | Stable string identifier for a network node |
| `NetworkArcId` | Stable string identifier that distinguishes parallel arcs |
| `NetworkNode<V>` | Node with generic `Quantity<V>` attributes; no routing-specific coordinate semantics |
| `NetworkArc<V>` | Directed arc with cost and generic attributes; negative costs are supported |
| `NetworkGraph<N, A>` | Immutable directed multigraph over generic payloads, with snapshotted identities, adjacency, filtering, and reverse views |
| `NetworkGraphAccess<N, A>` / `NetworkGraphBuilder<N, A>` | Identity/end-point selectors and validated graph construction |
| `NetworkTopology<N, A>` | Reachability, path witnesses, DAG ordering, SCCs, and target-aware shortest paths |
| `CapacityBounds<V>` | Non-negative lower and upper capacity bounds with unit validation |
| `NodeBalance<V>` | Signed node supply/demand quantity |
| `NetworkCost<V>` | Normalizable arc cost; negative values are supported |
| `NetworkSchedulingSolverValueAdapter<V>` | Domain-to-solver `Flt64` conversion boundary |
| `Flt64NetworkSchedulingSolverValueAdapter` | Built-in `Flt64` adapter singleton |
| `FltXNetworkSchedulingSolverValueAdapter` | Built-in `FltX` adapter singleton |

### Generic Path Search

| Type | Purpose |
|------|---------|
| `PathSearchContext` | Solver-independent finite-domain resource-label search |
| `PathProblem<N, A, S>` | Search definition with start, transition, terminal contributions, independent full-trace validation, filters, and optional domain-certified dominance |
| `PathSearchBudget` | Total/per-node label limits and cooperative interruption |
| `PathSearchResult<S, A>` | Path labels, trace reconstruction, output count, and honest completion status |

`NetworkGraph` snapshots node/arc identities and endpoints when built, so later payload mutation cannot silently rewrite adjacency. Parallel arcs are supported when their `NetworkArcId`s differ; self-loops are valid. `filtered(...)`, `filteredNodes(...)`, and `reverse()` preserve those identities. `NetworkTopology.shortestPath` uses Dijkstra for non-negative costs and Bellman-Ford when negative costs exist; it reports `NegativeCycle` only when a source-reachable negative cycle can reach the requested target.

`PathSearchContext` does not assume resource monotonicity and has no implicit dominance rule. Its start/transition/terminal callbacks can seed, extend, or reject a state; the separate `validate` callback independently checks the complete label trace rather than mixing that check into incremental transitions. Elementary searches retain access history; non-elementary cyclic walks require `maximumSteps`, while DAG labels are propagated in topological order without that bound. Caller-supplied dominance is additionally constrained by the elementary visited-set and remaining-step envelope. Uncertified dominance pruning or rejection by full-trace validation is reported explicitly, so `complete` does not claim exhaustive usable paths; validation `Ret` errors propagate. `maximumResults` limits only returned paths and does not stop search; label budgets or interruption do stop search and make `complete` false.

### VRP Domain

| Type | Purpose |
|------|---------|
| `Customer<V>`, `Depot<V>`, `VehicleType<V>` | Core domain entities with `Quantity<V>` attributes |
| `Route<V>`, `RouteStop<V>` | Elementary route with ordered stops, resource state, and cost |
| `VrptwInstance<V>` | Fully validated VRPTW instance (depots, customers, vehicle types, units, tolerances) |
| `VrptwSolution<V>` | Validated route set with aggregate distance and cost |
| `PricingDuals` | Immutable pricing dual snapshot (customer + fleet duals) |
| `RouteValidator` | Independent resource-recursion and cost validator |
| `BranchMask<K>`, `ResourceArc<K>` | Vehicle-resource branching constraints |
| `TravelTime`, `ServiceTimeWindow` | VRP travel and closed service-window value objects |

### Network Flow Domain

| Type | Purpose |
|------|---------|
| `FlowNode<V>`, `FlowArc<V>` | Generic flow graph elements and arc capacity bounds |
| `FlowCommodity<V>`, `FlowProblem<V>`, `FlowContext<V>` | Commodity balances, flow aggregate, and solver-variable registration |
| `SupplyDemand<V>` | Node ID to signed supply/demand binding |
| `FlowConservationConstraint` | Outflow minus inflow equals node balance |
| `CapacityBoundConstraint` | Shared lower/upper capacity across commodities |
| `MinCostFlowObjective` | Sum of flow multiplied by arc cost, including negative costs |

### Application

| Type | Purpose |
|------|---------|
| `VrptwApplicationService<V>` | Top-level entry: validates input, runs B&P, applies enrichers |
| `BranchAndPriceAlgorithm<V>` | Best-bound B&P orchestrator (queue-based, non-recursive) |
| `VrptwSolveResult<V>` | Public solve result with status, solution, bounds, gap, and trace |
| `BranchAndPriceStatus` | `Optimal \| Feasible \| Infeasible \| TimeLimit \| NodeLimit \| SolverStopped` |
| `BranchDecision` | Sealed class: `ForbidVehicleType`, `RequireVehicleType`, `ForbidArc`, `RequireArc` |


## Kotlin Delivery Status and Validation

This section records the Kotlin module scope only. It does not claim full-repository acceptance or native-solver benchmark results.

### Covered Kotlin Tests

The module-local tests cover:

- network infrastructure and flow model registration
- multigraph identities, reverse/filter topology, negative-edge shortest paths, DAG oracle comparisons, and generic path-search oracle comparisons
- VRPTW value validation, route validation, and pricing helpers
- ESPPRC label extension, resource feasibility, forbidden-customer reachability, dominance, branch masks, and column limits
- route compilation and application-level branch-and-price orchestration

Run the Kotlin tests for this module with:

```powershell
mvn -B -ntp -f ospf-kotlin-framework-network-scheduling/pom.xml test -T 0.75C
```

## Extension Points

All policies are injected fun interfaces; the framework never hard-codes a single strategy:

| Extension | Interface | Built-in |
|-----------|-----------|----------|
| Distance calculation | `DistanceCalculator<V>` | `EuclideanDistanceCalculator`, `SolomonDistancePolicy` |
| Travel time calculation | `TravelTimeCalculator<V>` | `DistanceAsTravelTimeCalculator` |
| Arc cost | `ArcCostCalculator<V>` | `DistanceArcCostCalculator` |
| Route cost aggregation | `RouteCostPolicy<V>` | `FixedPlusArcCostPolicy`, `Demo17CostPolicy` |
| Arc feasibility | `ArcFeasibilityPolicy<V>` | `DefaultArcFeasibilityPolicy` |
| ESPPRC label dominance | `LabelDominancePolicy` | `LabelDominancePolicy.Default` |
| Column selection | `PricingColumnSelector<V>` | `PricingColumnSelector.default()` |
| Progress trace | `BranchAndPriceTraceListener` | `BranchAndPriceTraceListener.None` |
| Post-solve processing | `SolutionEnricher<V>` | — (user-supplied) |
| Solver value conversion | `NetworkSchedulingSolverValueAdapter<V>` | `Flt64…`, `FltX…` |
| Extra CG pipelines | `CGPipeline<VrpShadowPriceArguments, …>` | — (user-supplied) |
| Route master-problem contributions | `RouteCompilationContribution<V>` via `BranchAndPriceAlgorithm.Policy.extraContributionFactory` | — (user-supplied) |

`extraContributionFactory(instance, model)` is called for each branch-node master model and must return fresh contribution instances for that model. A contribution participates in master registration, route-column add/remove, dual refresh and result extraction. Its `RouteArcCoefficientProvider.coefficients(arc)` supplies domain-valued, additive coefficients keyed by stable extra-row IDs; the same coefficients are aggregated for RMP columns and used in reduced-cost pricing, so keys must match those returned by `refreshShadowPrices`. Return each row explicitly even when its dual is zero: missing current duals or coefficients that cannot cross the solver boundary fail pricing instead of silently ignoring an extra row.

## Generic Numeric Boundaries

Domain APIs are generic over `V : RealNumber<V>`. All `Quantity<V>` attributes (distance, demand, capacity, balance, cost) preserve the caller's numeric type. `Flt64` is confined to:

- Solver adapter registration and variable/constraint extraction
- Dual values and reduced-cost computation
- `LinearMetaModel<Flt64>` at the solver boundary
- flow-context model registration, constraint construction, and objective construction at the solver boundary
- Test code

The `NetworkSchedulingSolverValueAdapter<V>` interface centralizes the conversion between domain `Quantity<V>` and solver `Flt64`. Domain models never reference `Flt64` directly.

## Physical Quantity Boundaries

| Quantity | Type | Unit source |
|----------|------|-------------|
| Distance | `Quantity<V>` | `VrptwUnits.distanceUnit` |
| Demand / Capacity | `Quantity<V>` | `VrptwUnits.loadUnit` |
| Fixed cost / Arc cost | `Quantity<V>` | `VrptwUnits.costUnit` |
| Travel time | `Duration` | `TimeWindow<V>` timeline |
| Service time | `Duration` | Per-customer |
| Ready / Due time | `Instant` | `TimeWindow<V>` timeline |
| Flow capacity / balance | `Quantity<V>` | Per-commodity and per-resource units with explicit cross-dimensional usage |

Bare numeric values are only used for dimensionless weights, ratios, and normalized solver scores. `NoneUnit` is used for cost when no business currency unit exists.

## Error Modes

All fallible public operations return `Ret<T>` (`Ok | Failed | Fatal`), never throw exceptions for validation failures:

- **Validation errors** (duplicate IDs, negative values, incompatible units, infeasible time windows): `Failed(ErrorCode.IllegalArgument, bilingualMessage)`
- **Algorithm terminal states** (`Infeasible`, `TimeLimit`, `NodeLimit`): returned as `Ok(VrptwSolveResult)` with honest bounds
- **Contract violations** (integrality invariant failure, solver call failure, unreliable duals): returned as `Failed`
- Error messages follow the format: `"操作失败：原因 / Operation failed: reason"`

## Quick Start

```kotlin
// Create instance, policies, and solver
val instance: VrptwInstance<Flt64> = ...
val service = VrptwApplicationService(
    instance = instance,
    solver = GurobiColumnGenerationSolver(),
    configuration = BranchAndPriceAlgorithm.Configuration(
        timeLimit = 300.seconds,
        nodeLimit = 100,
        relativeGapTolerance = Flt64(1e-4)
    ),
    policy = BranchAndPriceAlgorithm.Policy(
        valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
        distanceCalculator = EuclideanDistanceCalculator(Meter),
        travelTimeCalculator = DistanceAsTravelTimeCalculator(...),
        arcCostCalculator = DistanceArcCostCalculator(Meter, NoneUnit),
        routeCostPolicy = Demo17CostPolicy(NoneUnit)
    )
)

val result: Ret<VrptwSolveResult<Flt64>> = service.solve()
when (result) {
    is Ok -> {
        val solveResult = result.value
        println("Status: ${solveResult.status}")
        println("Routes: ${solveResult.solution?.routes?.size}")
        println("Total cost: ${solveResult.solution?.totalCost}")
    }
    is Failed -> println("Solve failed: ${result.error}")
    is Fatal -> println("Fatal: ${result.errors}")
}
```

## Local Validation

```powershell
mvn -B -ntp -f ospf-kotlin-framework-network-scheduling/pom.xml test -T 0.75C
mvn -B -ntp -pl ospf-kotlin-example -am -Pdemo5-gurobi-bp test -T 0.75C
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-network-scheduling -am -DskipTests package -T 0.75C
```

## Related

- [Migration guide](MIGRATION.md) — public API migration and proof boundaries
- [ospf-kotlin-framework](../ospf-kotlin-framework/README.md) — shared solver, pipeline, and remote-solver abstractions
- [ospf-kotlin-framework-gantt-scheduling](../ospf-kotlin-framework-gantt-scheduling/README.md) — Gantt scheduling framework (shared `TimeWindow<V>` infrastructure)

## Business Construction And Path Selection

The following optional domain modules extend Network without introducing a second scheduling framework:

| Module | Public entry | Business use |
|---|---|---|
| [allocation-context](network-scheduling-domain-allocation-context/README.md) | `AllocationProblem<V>`, `AllocationContext.create` | Compatible transport allocation, quotas, transshipment layers and optional hubs; compiles to Flow/Design |
| [path-compilation-context](network-scheduling-domain-path-compilation-context/README.md) | `PathCompilationProblem<V, C>`, `PathCompilationContext<V, C>` | Binary whole-path choices, unsplittable orders, shared resources, ordered services and arc-service coverage |
| [structure-context](network-scheduling-domain-structure-context/README.md) | `NetworkStructureProblem<V>`, `NetworkStructureContext<V>` | Rooted directed tree, optional Steiner relays and eligible facility ownership/quotas |
| [generalized-flow-context](network-scheduling-domain-generalized-flow-context/README.md) | `GeneralizedFlowProblem<V>`, `GeneralizedFlowContext<V>` | Input/arrival quantities, transport losses, yields and explicit single-input conversions |

Dependency additions: `allocation -> design -> flow`, `structure -> design`, `path-compilation -> path -> infrastructure`, and `generalized-flow -> infrastructure`. All depend on the common framework/modeling foundation, never on Gantt tasks or application algorithms. The Network starter exposes these domains; embedded users may depend only on the domains needed. The optional Gantt bridge remains separate.

Standalone services are `NetworkAllocationApplicationService`, `NetworkPathApplicationService`, `NetworkStructureApplicationService` and `NetworkGeneralizedFlowApplicationService`. They create fresh per-model contexts and reuse the same registration path as embedded models:

```kotlin
val result = NetworkPathApplicationService(solver, adapter).solve(pathProblem)
val embedded = PathCompilationContext(pathProblem, adapter)
val registered = embedded.register(model)
```

### Whole-Path And Pricing Semantics

Every `PathColumn<V, C>` represents one complete indivisible choice and one binary variable. Its payload retains business activities, service order and any internal timing. `PathCompilationRow<V>` expresses exact/lower/upper coverage, resource usage or quotas with an explicit unit. Omitted coefficients mean zero; unknown row keys fail. Integer flow measurement steps in allocation still allow splitting between services; use binary path columns for indivisible orders.

Column cost and row coefficients are evaluated for the **complete path**, so start/terminal and non-additive contributions are supported without unsafe additive assumptions or automatic dominance certification. Registration, add/remove, objective cleanup, normalization and reduced-cost evaluation share that same column definition. A stable ID denotes identical payload and normalized economics; conflicting reuse fails. Removing and later reintroducing an ID creates a new model variable.

Dual snapshots bind the current context revision and caller-supplied pricing scope (including graph/rule/branch/fixing versions). Missing row duals, foreign/stale snapshots, changed model structure or bounds and scope mismatches fail. Callers must provide the dual result from the current LP solve; a raw `MetaDualSolution` does not carry its own solve provenance. Declare every priced constraint as a problem row: an unrepresented external constraint cannot be silently priced at zero. Full-path reduced cost is cost minus all declared signed row-dual contributions; it is not an optimality certificate.

An optimal MILP result proves optimality only over `candidateIds`, the final finite pool. `PathSearchResult.complete`, `ColumnGenerationBatch` evidence and binary-master optimality are distinct contracts. Truncated generation cannot prove absence of an improving path, and no new exact branch-and-price tree is implied. Capacity-resource scheduling across separate selected activities remains Gantt's job.

### Structure And Conversion Boundaries

Rooted-tree selection has explicit unique-parent and bounded-depth acyclicity constraints, not merely a low-cost preference; self-loops are excluded and parallel service IDs remain distinct. Optional facilities gate assignments and tree activity. Coverage ownership is separate from flow demand and does not invent deliveries. Construction cost uses declared service-level/facility costs, not an implicit operating-flow cost.

Generalized flow conserves outgoing **input** minus incoming **arrival output** against each node's declared supply. Business yield/loss and unit conversion are separate: cross-dimensional conversion requires an explicit output quantity per declared input quantity, not a dimensionless unit shortcut. Each arc has finite input capacity, including gain cycles. This is single-input conversion only, not multi-input BOM/MRP, process synchronization or repair scheduling.

Executable native-free business examples live in `AllocationContextTest`, `PathCompilationContextTest` (indivisible orders, ordered multimodal service and stable arc coverage), `NetworkStructureContextTest` and `GeneralizedFlowContextTest`. They use hand witnesses and small-domain enumeration independently of solver installation. Opt-in SCIP tests are selected explicitly with `-Pscip-integration-tests`; read the actual executed/skipped counts, not just BUILD SUCCESS. Runtime tests and logs are not repository artifacts.
