# ospf-kotlin-core-plugin

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

ospf-kotlin-core-plugin is the **solver plugin module** of the OSPF (Open Solver Platform Framework) Kotlin project. It provides concrete solver implementations that bridge the core solver abstraction layer to commercial and open-source optimization solvers, as well as metaheuristic algorithms.

All plugins follow a unified interface contract defined in `ospf-kotlin-core`, enabling transparent solver switching without modifying the optimization model.

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────────┐
│                          User Application                                  │
├────────────────────────────────────────────────────────────────────────────┤
│  ospf-kotlin-core  │  Solver abstraction (LinearSolver, QuadraticSolver)  │
├────────────────────────────────────────────────────────────────────────────┤
│                     ospf-kotlin-core-plugin                                │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────┐ ┌───────┐ ┌──────┐        │
│  │ Gurobi  │ │ Gurobi11 │ │  COPT  │ │CPLEX │ │Hexaly │ │ SCIP │        │
│  └─────────┘ └──────────┘ └────────┘ └──────┘ └───────┘ └──────┘        │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌────────────┐ ┌─────────────────┐  │
│  │MindOPT  │ │  MOSEK   │ │ LINGO  │ │  OPTVerse  │ │   Heuristic     │  │
│  └─────────┘ └──────────┘ └────────┘ └────────────┘ │ GA PSO GWO SCA  │  │
│                                                       │ SAA MVO ...     │  │
│                                                       └─────────────────┘  │
├────────────────────────────────────────────────────────────────────────────┤
│  ospf-kotlin-math  │  ospf-kotlin-utils  │  ospf-kotlin-core             │
└────────────────────────────────────────────────────────────────────────────┘
```

## Plugin Modules

### Mathematical Programming Solvers

| Module | Solver | Status | Linear | Quadratic | Column Generation | Benders Decomposition |
|--------|--------|--------|--------|-----------|-------------------|-----------------------|
| `ospf-kotlin-core-plugin-gurobi` | Gurobi 10+ | :white_check_mark: | Yes | Yes | Yes | Yes |
| `ospf-kotlin-core-plugin-gurobi11` | Gurobi 11+ | :white_check_mark: | Yes | Yes | Yes | Yes |
| `ospf-kotlin-core-plugin-copt` | COPT (杉数) | :white_check_mark: | Yes | Yes | Yes | Yes |
| `ospf-kotlin-core-plugin-cplex` | CPLEX | :white_check_mark: | Yes | Yes | Yes | Yes |
| `ospf-kotlin-core-plugin-hexaly` | Hexaly | :white_check_mark: | Yes | Yes | Yes | — |
| `ospf-kotlin-core-plugin-scip` | SCIP | :white_check_mark: | Yes | Yes | Yes | Yes |
| `ospf-kotlin-core-plugin-mindopt` | MindOPT (达摩院) | :white_check_mark: | Yes | Yes | Yes | Yes |
| `ospf-kotlin-core-plugin-mosek` | MOSEK | :construction: | Yes | — | — | — |
| `ospf-kotlin-core-plugin-lingo` | LINGO | :construction: | — | — | — | — |
| `ospf-kotlin-core-plugin-optverse` | OPTVerse | :construction: | — | — | — | — |

### Metaheuristic Algorithms

| Module | Algorithm | Status | Description |
|--------|-----------|--------|-------------|
| `ospf-kotlin-core-plugin-heuristic` | GA | :white_check_mark: | Genetic Algorithm — selection, crossover, mutation, migration |
| | PSO | :white_check_mark: | Particle Swarm Optimization — inertia weight, acceleration |
| | GWO | :white_check_mark: | Grey Wolf Optimizer — alpha/beta/delta leadership hierarchy |
| | SCA | :white_check_mark: | Sine Cosine Algorithm — sine-cosine position update |
| | SAA | :white_check_mark: | Simulated Annealing Algorithm — temperature scheduling |
| | MVO | :white_check_mark: | Multi-Verse Optimizer — white/wormhole traversal |
| | EVO | :construction: | Energy Valley Optimizer |
| | GCO | :construction: | Germinal Center Optimization |
| | HCA | :construction: | Hill Climbing Algorithm |
| | HS | :construction: | Harmony Search |
| | NS | :construction: | Neighborhood Search |
| | SOA | :construction: | Seagull Optimization Algorithm |
| | WarSO | :construction: | War Strategy Optimization Algorithm |
| | WCA | :construction: | Water Cycle Algorithm |

## Common Plugin Architecture

Each mathematical programming solver plugin follows the same layered structure:

```
┌──────────────────────────────────────────────────────┐
│  {Solver}LinearSolver / {Solver}QuadraticSolver     │  Public API (implements core solver interface)
├──────────────────────────────────────────────────────┤
│  {Solver}ColumnGenerationSolver                      │  Column generation strategy
│  {Solver}BendersDecompositionSolver                  │  Benders decomposition strategy
├──────────────────────────────────────────────────────┤
│  {Solver}SolverCallBack                              │  Callback management (configuration, solution analysis)
│  {Solver}Variable                                    │  Variable type mapping (Binary/Integer/Continuous)
│  {Solver}Constraint                                  │  Constraint sign mapping (where applicable)
├──────────────────────────────────────────────────────┤
│  {Solver}Solver (abstract)                           │  Base class — init, solve, analyzeStatus, close
│  PluginSolverAsync                                   │  Coroutine scope for async solving
└──────────────────────────────────────────────────────┘
```

### Solver Lifecycle

Every solver plugin follows a consistent lifecycle:

1. **Init** — Create solver environment and model (local or remote server)
2. **Dump** — Translate variables, constraints, and objectives into solver-native format
3. **Configure** — Apply solver parameters via callback hooks
4. **Solve** — Execute optimization
5. **AnalyzeStatus** — Map solver-native status to `SolverStatus`
6. **AnalyzeSolution** — Extract variable values, objective, and bound
7. **Close** — Release solver resources

### Callback System

Each solver provides a callback manager (`{Solver}SolverCallBack`) that supports hooking into the solving process at multiple points:

| Point | Description |
|-------|-------------|
| `AfterModeling` | After model construction, before solving |
| `Configuration` | Solver parameter configuration phase |
| `AnalyzingSolution` | Post-solve solution analysis |
| `AfterFailure` | After solving failure |

## Heuristic Algorithm Architecture

The heuristic module implements a **policy-driven** design:

```
┌────────────────────────────────────────────┐
│  {Algorithm}Algorithm                      │  Main algorithm runner
│    ├─ policy: Abstract{Algorithm}Policy    │  Strategy interface
│    ├─ invoke(model, runningCallBack)       │  Execute algorithm
│    └─ Iteration tracking, good solutions   │
├────────────────────────────────────────────┤
│  {Algorithm}Policy                         │  Concrete policy
│    ├─ HeuristicPolicy (base)               │  Iteration/time limits
│    └─ AbstractHeuristicPolicy              │  coerceIn, update, finished
├────────────────────────────────────────────┤
│  Population / Particle / Wolf / Chromosome │  Individual representations
│    └─ Individual<ObjValue, V>              │  Common interface
└────────────────────────────────────────────┘
```

All heuristic algorithms support:
- **Generic value types** — works with any `RealNumber<V>` + `NumberField<V>`
- **Single- and multi-objective** — via type aliases (`GA` / `MulObjGA`)
- **Iteration and time limits** — configurable stopping criteria
- **Running callbacks** — observe progress at each iteration
- **Memory pressure handling** — automatic cleanup between iterations

## Usage

### Adding a Solver Plugin Dependency

```kotlin
// build.gradle.kts
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi:1.1.0")
```

### Using a Solver

```kotlin
val solver = GurobiLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds),
    callBack = GurobiLinearSolverCallBack().configuration { status, gurobi, variables, constraints ->
        gurobi.set(GRB.IntParam.Threads, 4)
        ok
    }
)
val result = solver(model)
```

### Using a Heuristic Algorithm

```kotlin
val ga = GA(
    population = listOf(
        PopulationBuilder(
            eliteAmount = UInt64(2),
            densityRange = ValueRange(UInt64(20), UInt64(50)),
            mutationRateRange = ValueRange(Flt64(0.01), Flt64(0.1)),
            parentAmountRange = ValueRange(UInt64(2), UInt64(4))
        )
    ),
    migrationPeriod = UInt64(10),
    policy = GAPolicy(...)
)
val bestSolutions = ga(model)
```

## Dependencies

- `ospf-kotlin-core` — Solver abstraction interfaces and model types
- `ospf-kotlin-math` — Mathematical types (Flt64, UInt64, algebra concepts)
- `ospf-kotlin-utils` — Functional utilities (Ret, Try, Order)

## Testing

```bash
# Test all plugins
mvn -pl ospf-kotlin-core-plugin test -DskipITs

# Test a specific plugin
mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi test -DskipITs
mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic test -DskipITs
```

## Notes

- **Gurobi 10 vs Gurobi 11**: The `gurobi` module uses `gurobi.*` imports (Gurobi 10 API), while `gurobi11` uses `com.gurobi.gurobi.*` (Gurobi 11+ API with relocated package). Choose the module matching your Gurobi installation.
- **SCIP Native Library**: The SCIP plugin loads native libraries via JNA. Use `ScipSolver.loadLibraryInJar()` for JAR-packaged deployments.
- **Remote Solving**: Gurobi and COPT plugins support remote server connections with authentication.
- **Solver Licenses**: Each commercial solver requires its own license. The framework does not bundle solver binaries.
