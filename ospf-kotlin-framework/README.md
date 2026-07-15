# OSPF Kotlin Framework

:us: English | :cn: [简体中文](README_ch.md)

`ospf-kotlin-framework` is the shared infrastructure module for OSPF Kotlin domain frameworks (CSP1D, BPP3D, Gantt Scheduling, etc.). It provides solver abstractions, pipeline modeling, persistence expression DSL, network utilities, log context, and running heartbeat tracking.

## Scope

This module covers:

1. **Solver abstractions** — column generation, Benders decomposition, and combinatorial solver interfaces with MILP/LP solving, async variants, and generic value conversion.
2. **Pipeline modeling** — constraint pipeline, column generation pipeline, and heuristic analysis pipeline interfaces; shadow price key/price/map/extractor hierarchy.
3. **Persistence expression DSL** — SQL predicate pushdown annotations, repository API, sorting, update assignment, and scalar function DSL for Ktorm integration.
4. **Network utilities** — HTTP response sending with retry mechanism and authorization support.
5. **Log context** — log pushing/saving interfaces and builder-pattern context management.
6. **Running heartbeat** — sub-progress, running, and finish heartbeat data structures for long-running solve tracking.
7. **Remote solver** — time-sliced remote solving with checkpoint, object storage port, and solver execution port.

Explicit non-goals:

1. Domain-specific modeling (cutting plans, packing, scheduling) — these belong in `ospf-kotlin-framework-*` domain modules.
2. Solver backend implementations — these belong in `ospf-kotlin-core-plugin-*`.
3. Persistence backend implementations — these belong in `ospf-kotlin-framework-plugin-persistence-*`.

## Package Overview

| Package | Responsibility |
| --- | --- |
| `solver` | Column generation, Benders decomposition, combinatorial solver interfaces; solve options; number type aliases; async scope |
| `solver.remote` | Remote solver client, domain models, ports, and adapters for time-sliced solving |
| `model` | Pipeline interfaces (constraint / column generation / heuristic analysis); shadow price hierarchy |
| `persistence` | Request/response DTOs, log record DAO, persistence API controller, SQL type extensions |
| `persistence.expression` | Predicate pushdown annotations, repository API, sorting DSL, update assignment DSL, scalar function DSL |
| `network` | HTTP response utilities with retry and authorization |
| `log` | Log context management (push/save interfaces, builder) and log record types |

## Solver Abstractions

### ColumnGenerationSolver

The core interface for column generation solving:

```kotlin
interface ColumnGenerationSolver {
    val name: String

    // MILP solving
    suspend fun solveMILP(name: String, metaModel: Flt64LinearMetaModel, ...): Ret<Flt64FeasibleSolverOutput>
    suspend fun solveMILP(metaModel: Flt64LinearMetaModel, options: FrameworkSolveOptions): Ret<Flt64FeasibleSolverOutput>

    // LP solving (returns dual solution for pricing)
    suspend fun solveLP(name: String, metaModel: Flt64LinearMetaModel, ...): Ret<LPResult>

    // Async variants (CompletableFuture)
    fun solveMILPAsync(...): CompletableFuture<Ret<Flt64FeasibleSolverOutput>>
    fun solveLPAsync(...): CompletableFuture<Ret<LPResult>>

    // Value conversion variants (Flt64 -> V)
    suspend fun <V> solveMILPAs(name: String, metaModel: Flt64LinearMetaModel, converter: IntoValue<V>, ...): Ret<FeasibleSolverOutput<V>>
    suspend fun <V> solveLPAs(name: String, metaModel: Flt64LinearMetaModel, converter: IntoValue<V>, ...): Ret<LPResultOf<V>>
}
```

`LPResult` bundles the feasible solver output with the constraint dual solution map, which is essential for column generation pricing.

### BendersDecompositionSolver

Linear and quadratic Benders decomposition interfaces:

```kotlin
interface LinearBendersDecompositionSolver {
    val name: String
    suspend fun solveMaster(metaModel: Flt64LinearMetaModel, ...): Ret<Flt64FeasibleSolverOutput>
    suspend fun solveSub(metaModel: Flt64LinearMetaModel, ...): Ret<LinearSubResult>
}
```

`LinearSubResult` is a sealed interface with `Feasible` and `Infeasible` variants, following the Benders decomposition pattern.

### Combinatorial Solvers

Run multiple solvers and select a result:

| Solver | Strategy | Result selection |
| --- | --- | --- |
| `SerialCombinatorialLinearSolver` | Run solvers serially | First success |
| `ParallelCombinatorialLinearSolver` | Run solvers in parallel | `First` or `Best` (via `ParallelCombinatorialMode`) |
| `SerialCombinatorialColumnGenerationSolver` | Run CG solvers serially | First success |
| `ParallelCombinatorialColumnGenerationSolver` | Run CG solvers in parallel | `First` or `Best` |

### FrameworkSolveOptions

Unified solve options with builder pattern:

```kotlin
val options = FrameworkSolveOptions.Builder()
    .solveName("my-solve")
    .toLogModel(true)
    .solutionAmount(UInt64(3))
    .registrationStatusCallBack(myCallback)
    .solvingStatusCallBack(mySolvingCallback)
    .build()
```

### Remote Solver

The `solver.remote` sub-package provides time-sliced remote solving with checkpoint support:

- **domain** — value types (TaskId, SliceId, etc.), error codes, serialized/normalized/storage/task models
- **port** — `ObjectStoragePort` (put/get/delete/exists) and `SolverExecutionPort` (start/resume/await/export/fetch/stop)
- **client** — `RemoteSolverClient` implements round-by-round solving with checkpoint; `RemoteSolverHttpClient` for HTTP transport
- **adapter** — `LocalFileObjectStoragePort` for local filesystem storage; `OspfRemoteModelSerializer` for OSPF serialization format

## Model Abstractions

### Pipeline Hierarchy

```
Pipeline<M>              — constraint pipeline: register() + invoke()
├── CGPipeline<Args, Model, Map>  — column generation pipeline: adds extractor() + refresh()
└── HAPipeline<M>        — heuristic analysis pipeline: adds calculate() + check()
```

- `Pipeline<M>` — registers constraint groups to a `MetaModel` and executes them.
- `CGPipeline<Args, Model, Map>` — extends `Pipeline` with shadow price extraction and refresh for column generation pricing.
- `HAPipeline<M>` — extends `Pipeline` with heuristic objective calculation and solution validity checking.

`PipelineList<M>`, `CGPipelineList<Args, Model, Map>`, and `HAPipelineList<M>` type aliases support batch execution with `operator fun invoke(model: M): Try`.

### Shadow Price System

```
ShadowPriceKey(limit: KClass<*>)
  ↓
ShadowPrice(key, price: Flt64)
  ↓
AbstractShadowPriceMap<Args, M>  — put/get/putOrAdd/remove/shrink + extractor registration
  ↓
ShadowPriceExtractor<Args, M> = (AbstractShadowPriceMap, Args) -> Flt64
```

`extractShadowPrice()` refreshes shadow prices from a `CGPipelineList` and registers extractors. `IntermediateSymbol<*>.refresh()` handles intermediate symbol dual values.

## Persistence Expression DSL

The `persistence.expression` sub-package provides a type-safe SQL predicate pushdown framework with KSP annotation processing. See the dedicated documentation:

- [persistence/expression/README.md](src/main/fuookami/ospf/kotlin/framework/persistence/expression/README.md)

Key components:

- `@PredicateEntity` / `@PredicateField` — KSP annotations for schema generation
- `ExpressionRepository<E>` — repository interface with find/count/exists/update/delete
- `SortBy` / `SortItem` — sorting DSL with nulls order support
- `UpdateAssignments` — UPDATE SET DSL (set/setNull/setExpr)
- `UnsupportedPredicatePolicy` — handling strategies for non-pushdown predicates
- `ScalarFunctionDsl` — SQL scalar functions (lower, upper, trim, length, coalesce)

## Network Utilities

`Response.kt` provides HTTP response sending with retry:

- `Authorization` interface and `BasicAuthorization` data class
- `ResponseRetry` configuration (max attempts, delay)
- `response()` extension functions for POST with retry

## Log Context

`LogContext.kt` provides log context management:

- `Pushing` / `Saving` interfaces for log push/save operations
- `LogContext` with builder pattern for assembling push/save handlers
- `LogRecordPO<T>` for log record persistence

`LogRecord.kt` (in `log/` package) defines:

- `LogRecordType` enum (Info, Warning, Error, Fetal)
- `LogRecordRPO` / `LogRecordRDAO` for Ktorm entity and table definition

## Running Heartbeat

`RunningHeartBeat.kt` defines three heartbeat data structures for long-running solve progress tracking:

- `SubProgressHeartBeat` — estimated time, progress percentage, optional message
- `RunningHeartBeat` — task ID, run time, estimated time, optimization rate, timestamp
- `FinnishHeartBeat` — task ID, total run time, status code, message, timestamp; companion factory methods for success and error cases

## Usage Examples

### Column Generation with Pipeline

```kotlin
// Define constraint pipelines
val constraintPipelines: PipelineList<LinearMetaModel<Flt64>> = listOf(
    DemandPipeline(),
    CapacityPipeline(),
    YieldPipeline()
)

// Register and execute all pipelines
val ret = constraintPipelines(metaModel)
if (ret is Failed) return ret

// Solve LP for pricing
val lpResult = solver.solveLP(
    name = "pricing-lp",
    metaModel = metaModel
)

// Extract shadow prices
val shadowPriceMap = MyShadowPriceMap()
val refreshRet = extractShadowPrice(
    shadowPriceMap = shadowPriceMap,
    pipelineList = cgPipelines,
    model = metaModel,
    shadowPrices = lpResult.value.dualSolution
)
```

### Combinatorial Solving

```kotlin
// Try multiple solvers in parallel, take the best result
val combinatorialSolver = ParallelCombinatorialColumnGenerationSolver(
    solvers = listOf(solver1, solver2),
    mode = ParallelCombinatorialMode.Best
)
val result = combinatorialSolver.solveMILP(metaModel = metaModel)
```

### Value Conversion

```kotlin
// Solve with Flt64 and convert result to FltX
val result = solver.solveMILPAs<FltX>(
    name = "my-solve",
    metaModel = metaModel,
    converter = FltX.toIntoValue()
)
```

## Local Validation

Compile the framework module:

```powershell
mvn -B -ntp -pl ospf-kotlin-framework -DskipTests compile
```

Run framework tests:

```powershell
mvn -B -ntp -pl ospf-kotlin-framework test
```
