# model — Optimization Model Layer

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `model` package is the **core model layer** of the OSPF framework, implementing the complete lifecycle management of mathematical optimization models. It employs a **four-layer architecture**, progressively transforming from user-friendly MetaModels to solver-consumable standard-form models (Triad/Tetrad Models), which are then submitted to solvers.

## Four-Layer Architecture

```
User Definition Layer              MetaModel<V>             ← Users build models here
    ↓ dump
Mechanism Model Layer         MechanismModel<V>           ← Flatten intermediate symbols, register constraints
    ↓ dump
Standard Form Layer     LinearTriadModel / QuadraticTetradModel  ← Sparse matrix form
    ↓ invoke
Solver Layer                    SolverOutput               ← Solve results
```

## Package Structure

```
model/
├── basic/        # Foundation layer — Interfaces, enums, view types
├── mechanism/    # Mechanism layer — Meta model and mechanism model
├── intermediate/ # Intermediate layer — Standard form models (triad/tetrad)
└── callback/     # Callback layer — Callback model interfaces for heuristic solvers
```

## basic/ — Foundation Layer

Defines foundational interfaces and enum types for the model system.

### Core Interfaces (`Model.kt`)

Model interface hierarchy:

```
Model<V>                    — Base model interface (variable registration, objective setting, solution management)
├── LinearModel<V>          — Linear model (linear constraints, linear objective)
├── QuadraticModel<V>       — Quadratic model (quadratic constraints, quadratic objective)
└── ExpressionModel<V>      — Expression model (intermediate symbol management)
```

`Model<V>` core capabilities:
- `add(variable)` — Register decision variables
- `addObject()` / `minimize()` / `maximize()` — Set objective function
- `setSolution()` / `clearSolution()` — Manage solve results

### Constraints and Objectives

- **`ConstraintPriority`** (`ConstraintPriority.kt`) — Constraint priority enum (`Mandatory`, `Suggested`, `NiceToHave`, etc.)
- **`ConstraintSign`** (`ConstraintSign.kt`) — Constraint sign (`LessEqual`, `Equal`, `GreaterEqual`)
- **`ObjectCategory`** (`ObjectCategory.kt`) — Objective category (`Minimum`, `Maximum`)
- **`MultiObject`** (`MultiObject.kt`) — Multi-objective optimization support

### Model Views

- **`ModelView`** (`ModelView.kt`) — Model view interface, providing read-only access to models
- **`ExpressionRange`** (`ExpressionRange.kt`) — Expression value domain, including lower bound, upper bound, and fixed value

### Others

- **`ModelBuildingStage`** / **`ModelBuildingStatus`** — Model building stage and status
- **`ModelFileFormat`** — Model file format (LP, MPS, etc.)
- **`RegistrationStatus`** — Variable/symbol registration status callback

## mechanism/ — Mechanism Model Layer

The mechanism model layer is the core bridge between users and solvers, handling model flattening, constraint building, and objective management.

### MetaModel (`MetaModel.kt`)

The **meta model** is the entry point for users to build optimization models. Users register variables, intermediate symbols, constraints, and objectives here.

Core capabilities:
- Inherits `Model<V>`, providing complete variable and objective management
- Supports intermediate symbol registration and management
- Constraint input built via `MetaConstraint` DSL

### MechanismModel (`MechanismModel.kt`)

The **mechanism model** is the product of MetaModel flattening, in a solver-consumable form. The flattening process includes:
- Polynomial expansion of intermediate symbols
- Constraint flattening and standardization
- Objective function expansion

Subtypes:
- `LinearMechanismModel<V>` — Linear mechanism model
- `QuadraticMechanismModel<V>` — Quadratic mechanism model

### Constraint System

- **`Constraint`** (`Constraint.kt`) — Constraint data structure
- **`MetaConstraint`** (`MetaConstraint.kt`) — Meta constraint (user-layer constraint DSL)
- **`LinearConstraintInput`** (`LinearConstraintInput.kt`) — Linear constraint input
- **`Relation`** (`Relation.kt`) — Constraint relation definition
- **`MathInequalityDsl`** / **`MathInequalityFlatten`** — Math inequality DSL and flattening

### Objective System

- **`Object`** (`Object.kt`) — Objective function definition
- **`SubObject`** (`SubObject.kt`) — Sub-objective (multi-objective optimization)

### Auxiliary Features

- **`MechanismModelDumpSupport`** — Model dumping support
- **`MechanismModelCutSupport`** — Cutting plane support
- **`MechanismModelObjectiveSupport`** — Objective function support
- **`MechanismModelFlt64Conversion`** — Flt64 type conversion
- **`MetaModelExportSupport`** — Model export support (LP/MPS format)

## intermediate/ — Standard Form Model Layer

Converts mechanism models into solver-consumable sparse matrix form.

### LinearTriadModel (`LinearTriadModel.kt`)

The **linear triad model** is the standard form for linear programming:

```
min/max  c^T x
s.t.     A x {≤,=,≥} b
         l ≤ x ≤ u
```

Stores constraint coefficients, objective vector, and bounds in sparse matrix form.

### QuadraticTetradModel (`QuadraticTetradModel.kt`)

The **quadratic tetrad model** is the standard form for quadratic programming, adding a quadratic term matrix on top of the triad model.

### Sparse Matrix (`SparseMatrix.kt`)

Efficient sparse matrix implementation for storing constraint coefficient matrices.

### Dump Builders

- **`LinearTriadDumpBuilders`** — Builders for linear triad model
- **`LinearTriadElasticBuilder`** — Elastic constraint builder (auto-relaxation)
- **`QuadraticTetradDumpBuilders`** — Builders for quadratic tetrad model
- **`QuadraticTetradElasticBuilder`** — Quadratic elastic constraint builder
- **`DumpHelpers`** — Dump helper utilities

### Others

- **`Cell`** (`Cell.kt`) — Matrix cell
- **`BatchDispatchPolicy`** — Batch dispatch policy
- **`MemoryCleanupPolicy`** — Memory cleanup policy
- **`TriadDualSolverSupport`** — Dual solving support
- **`IntermediateModelDumpingStatus`** / **`MechanismModelDumpingStatus`** — Dumping status

## callback/ — Callback Model Layer

Callback model interfaces provided for heuristic/metaheuristic solvers.

- **`CallBackModel`** (`CallBackModel.kt`) — Callback model implementation
- **`CallBackModelInterface`** (`CallBackModelInterface.kt`) — Callback model interface definition

Heuristic solvers obtain objective function values and constraint violations through callback interfaces, rather than directly operating on sparse matrices.

## Data Flow

```
User Code
  │
  ▼
MetaModel<V>
  │  add() variables, symbols, constraints, objectives
  │
  ▼  dump()
MechanismModel<V>
  │  Flatten intermediate symbols → linear/quadratic polynomials
  │  Flatten constraints → standard inequalities
  │
  ▼  dump()
LinearTriadModel / QuadraticTetradModel
  │  Sparse matrix A, vectors b, c, bounds l/u
  │
  ▼  invoke()
SolverOutput
  │  obj, solution, time, gap, ...
  │
  ▼  setSolution()
TokenTable → Token.result (V?)
```

## Relationships with Other Packages

- **variable** — Models register decision variables via `Model.add()`
- **token** — Models manage variable-to-solver-index mapping and solve results via `TokenTable`
- **symbol** — Models register intermediate symbols; flattening invokes symbol polynomial conversion
- **solver** — Solvers consume standard form models (Triad/Tetrad) and return `SolverOutput`