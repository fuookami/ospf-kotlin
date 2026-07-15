# ospf-kotlin-core

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

ospf-kotlin-core is the **core module** of the OSPF (Open Solver Platform Framework) Kotlin project. It implements the complete mathematical optimization model lifecycle — from variable definition and symbol expression construction, through model building and flattening, to solver abstraction and result retrieval.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      User Application                       │
├─────────────────────────────────────────────────────────────┤
│  model/       │ Optimization model definition & management  │
│  variable/    │ Variable type system & variable items       │
│  symbol/      │ Intermediate symbol expressions & functions │
│  token/       │ Variable-solver mapping & caching           │
│  solver/      │ Solver abstraction & value conversion       │
│  error/       │ Core error definitions                      │
├─────────────────────────────────────────────────────────────┤
│  ospf-kotlin-math  │ ospf-kotlin-utils  │ ospf-kotlin-multiarray │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

| Package | Description | README |
|---------|-------------|--------|
| `variable` | Variable type system, variable items, combinations, and ranges | [README](src/main/fuookami/ospf/kotlin/core/variable/README.md) |
| `token` | Token management — variable-solver mapping, dual-view results, multi-level caching | [README](src/main/fuookami/ospf/kotlin/core/token/README.md) |
| `symbol` | Intermediate symbol system — expressions, combinations, and function symbols | [README](src/main/fuookami/ospf/kotlin/core/symbol/README.md) |
| `model` | Optimization model lifecycle — MetaModel → MechanismModel → Triad/TetradModel → Solver | [README](src/main/fuookami/ospf/kotlin/core/model/README.md) |
| `solver` | Solver abstraction — linear/quadratic solvers, heuristics, IIS diagnostics, output | [README](src/main/fuookami/ospf/kotlin/core/solver/README.md) |
| `error` | Core error code definitions | — |

## Four-Layer Model Architecture

The core module implements a **four-layer model architecture**:

```
User Definition Layer    →  MetaModel<V>                  (user-facing DSL)
    ↓ dump
Mechanism Model Layer    →  MechanismModel<V>             (flatten symbols, constraints)
    ↓ dump
Standard Form Layer      →  LinearTriadModel /            (sparse matrix form)
                            QuadraticTetradModel
    ↓ invoke
Solver Layer             →  SolverOutput                  (results)
```

## Key Design Patterns

### Dual-View Value Access

Solver backends always produce `Flt64` results. The framework provides type-safe access via `IntoValue<V>`:

```
Solver → Flt64 → Token._result → IntoValue<V> → Token.result (V?)
```

### Intermediate Symbol System

Intermediate symbols are evaluable expressions composed of variables and constants. The framework provides 30+ built-in function symbols (Slack, If, Max, Piecewise, etc.) for constraint construction.

### Cache-Driven Evaluation

The `TokenTable` maintains multi-level caches (linear flatten, quadratic flatten, value, range) to avoid redundant symbol evaluation during model construction and solving.

## Sub-package Overview

### solver/

| Sub-package | Description |
|-------------|-------------|
| `config` | Solver-specific configuration (COPT, Gurobi, SCIP) |
| `heuristic` | Metaheuristic framework (PSO, selection, crossover, mutation) |
| `iis` | Irreducible Infeasible Subsystem diagnostics |
| `output` | Solver output data structures (feasible/infeasible) |
| `value` | Value type conversion (IntoValue interface) |

### model/

| Sub-package | Description |
|-------------|-------------|
| `basic` | Foundation interfaces, enums, and view types |
| `mechanism` | MetaModel, MechanismModel, constraint/objective DSL |
| `intermediate` | Standard form models (triad/tetrad), sparse matrix |
| `callback` | Callback model interface for heuristic solvers |

### symbol/

| Sub-package | Description |
|-------------|-------------|
| `function` | 30+ function symbols (Slack, If, Max, Piecewise, etc.) |
| `flatten` | Expression flattening utilities |

## Testing

```bash
mvn -pl ospf-kotlin-core test -DskipITs