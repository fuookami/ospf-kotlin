# model

[中文文档 (README_ch.md)](./README_ch.md)

The `model` package defines the three-layer model pipeline for LP/QP optimization in OSPF Kotlin. Each layer has a distinct responsibility in transforming user-level models into solver-ready matrix representations.

## Import

```kotlin
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.callback.*
```

## Architecture

```
User Code
    │
    ▼
┌─────────────────────────────────────────┐
│  basic/   — Core interfaces & enums     │  Model, LinearModel, QuadraticModel,
│            (solver-agnostic)            │  ConstraintRelation, ObjectCategory,
│                                         │  ExpressionRange, ModelBuildingStage
└──────────────────┬──────────────────────┘
                   │
    ┌──────────────┴──────────────┐
    ▼                             ▼
┌──────────────────┐   ┌──────────────────┐
│ mechanism/       │   │ callback/        │
│ MetaModel →      │   │ CallBackModel    │
│ MechanismModel   │   │ (function-based) │
│ (symbolic layer) │   │ (heuristic)      │
└────────┬─────────┘   └──────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  intermediate/ — Sparse matrix models   │
│  LinearTriadModel / QuadraticTetradModel│
│  (solver-ready)                         │
└──────────────────┬──────────────────────┘
                   │
                   ▼
           Solver (LP/QP)
```

## Sub-packages

| Sub-package | Description | Docs |
|---|---|---|
| [basic/](./basic/) | Core interfaces (`Model`, `LinearModel`, `QuadraticModel`), enums (`ConstraintRelation`, `ObjectCategory`, `ModelBuildingStage`), and types (`ExpressionRange`, `Variable`, `Objective`) | [EN](./basic/README.md) · [中文](./basic/README_ch.md) |
| [mechanism/](./mechanism/) | Symbolic model layer: `MetaModel` → `MechanismModel` pipeline with constraint registration, inequality DSL, and multi-objective support | [EN](./mechanism/README.md) · [中文](./mechanism/README_ch.md) |
| [intermediate/](./intermediate/) | Sparse matrix model layer: `LinearTriadModel` / `QuadraticTetradModel` with constraint cells, objective vectors, and solver-ready views | [EN](./intermediate/README.md) · [中文](./intermediate/README_ch.md) |
| [callback/](./callback/) | Function-based model layer: `CallBackModel` / `MultiObjectCallBackModel` for heuristic solvers where constraints and objectives are evaluated as functions of a solution vector | [EN](./callback/README.md) · [中文](./callback/README_ch.md) |

## Layer Responsibilities

### basic/

Defines solver-agnostic core interfaces and enums shared by all model types. Contains no solver-specific logic.

### mechanism/

Symbolic layer where users register variables (Tokens), define constraints using DSL operators (`geq`, `leq`, `eq`), and set objectives. `MetaModel` holds symbolic expressions; `MechanismModel` flattens them into a Token table representation.

### intermediate/

Converts mechanism models into sparse matrix form (`LinearTriadModel` / `QuadraticTetradModel`) for direct consumption by LP/QP solvers. Handles batch scheduling, memory cleanup policies, and dual solution extraction.

### callback/

Alternative model path for heuristic solvers. Instead of building explicit matrices, constraints and objectives are defined as functions that evaluate directly on a solution vector.
