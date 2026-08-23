# ospf-kotlin-framework-network-scheduling

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-framework-network-scheduling` provides generic network infrastructure for VRP and other network-flow problem families, plus a complete VRPTW (Vehicle Routing Problem with Time Windows) Branch-and-Price solver. It includes immutable network graphs, generic flow modeling with capacity and balance constraints, ESPPRC exact pricing, incremental column generation with Phase I/II, and a best-bound Branch-and-Price orchestrator.

## Module Structure

```
ospf-kotlin-framework-network-scheduling/
├── network-scheduling-infrastructure/          # Generic graph, arc, capacities, balances, value adapter
├── network-scheduling-domain-vrp-context/      # VRPTW instance, routes, validators, cost policies
├── network-scheduling-domain-flow-context/     # Generic single-/multi-commodity network flow
├── network-scheduling-domain-route-generation-context/  # ESPPRC pricer, pricing graph, label dominance
├── network-scheduling-domain-route-compilation-context/ # Column pool, CG lifecycle, shadow prices
└── network-scheduling-application/             # B&P algorithm, application service, solve result
```

Dependency graph (arrows mean "depends on"):

```
infrastructure
     ↑
     ├── domain-vrp-context ──→ gantt-scheduling-infrastructure
     ├── domain-flow-context ──→ framework / quantities
     ├── domain-route-generation-context ──→ domain-vrp-context
     └── domain-route-compilation-context ──→ domain-vrp-context
                                      ↑
                                 application
```

`domain-route-generation` and `domain-route-compilation` do not depend on each other; `application` bridges them by passing `PricingDuals` from compilation to generation.

## Public API

### Infrastructure

| Type | Purpose |
|------|---------|
| `NetworkNodeId` | Stable string identifier for a network node |
| `NetworkNode<V>` | Node with generic `Quantity<V>` attributes; no routing-specific coordinate semantics |
| `NetworkArc<V>` | Directed arc with cost and generic attributes; negative costs are supported |
| `NetworkGraph<V>` | Immutable directed graph with stable indices, adjacency access, and predicate filtering |
| `CapacityBounds<V>` | Non-negative lower and upper capacity bounds with unit validation |
| `NodeBalance<V>` | Signed node supply/demand quantity |
| `NetworkCost<V>` | Normalizable arc cost; negative values are supported |
| `NetworkSchedulingSolverValueAdapter<V>` | Domain-to-solver `Flt64` conversion boundary |
| `Flt64NetworkSchedulingSolverValueAdapter` | Built-in `Flt64` adapter singleton |
| `FltXNetworkSchedulingSolverValueAdapter` | Built-in `FltX` adapter singleton |

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
| `FlowCommodity<V>`, `FlowGraph<V>` | Commodity balances, flow aggregate, and solver-variable registration |
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
| Flow capacity / balance | `Quantity<V>` | Flow graph `flowUnit` |

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

- [daily.md](daily.md) — phased implementation plan and mathematical contract
- [ospf-kotlin-framework](../ospf-kotlin-framework/README.md) — shared solver, pipeline, and remote-solver abstractions
- [ospf-kotlin-framework-gantt-scheduling](../ospf-kotlin-framework-gantt-scheduling/README.md) — Gantt scheduling framework (shared `TimeWindow<V>` infrastructure)
