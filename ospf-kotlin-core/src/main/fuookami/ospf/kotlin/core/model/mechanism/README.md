# model/mechanism — Mechanism Model Layer

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `mechanism` sub-package is the **core bridge between users and solvers** in the OSPF framework. It contains the MetaModel (user-facing model builder), MechanismModel (flattened solver-ready model), and the constraint/objective DSL system. This layer handles intermediate symbol flattening, constraint standardization, and objective expansion.

## Package Structure

```
mechanism/
├── MetaModel.kt                      # User-facing model builder
├── BasicModel.kt                     # Basic model implementation
├── BasicMechanismModel.kt            # Basic mechanism model
├── MechanismModel.kt                 # Flattened mechanism model
├── Constraint.kt                     # Constraint data structure
├── MetaConstraint.kt                 # Meta constraint DSL
├── LinearConstraintInput.kt          # Linear constraint input
├── Relation.kt                       # Constraint relation definition
├── Object.kt                         # Objective function definition
├── SubObject.kt                      # Sub-objective (multi-objective)
├── MathInequalityDsl.kt             # Math inequality DSL
├── MathInequalityFlatten.kt         # Math inequality flattening
├── MechanismModelDumpSupport.kt      # Model dump support
├── MechanismModelCutSupport.kt       # Cutting plane support
├── MechanismModelObjectiveSupport.kt # Objective support
├── MechanismModelFlt64Conversion.kt # Flt64 type conversion
└── MetaModelExportSupport.kt         # Model export (LP/MPS)
```

## Core Concepts

### MetaModel (`MetaModel.kt`)

The **meta model** is the entry point for users to build optimization models. It provides:
- Variable registration via `Model.add()`
- Intermediate symbol registration
- Constraint building via `MetaConstraint` DSL
- Objective setting via `minimize()` / `maximize()`

### MechanismModel (`MechanismModel.kt`)

The **mechanism model** is the flattened, solver-ready form produced by dumping a MetaModel. The flattening process:
1. Expands intermediate symbols into linear/quadratic polynomials
2. Flattens constraints into standard inequalities
3. Expands objective functions

Subtypes:
- `LinearMechanismModel<V>` — Linear mechanism model
- `QuadraticMechanismModel<V>` — Quadratic mechanism model

### Constraint System

- **`Constraint`** — Core constraint data structure with priority, sign, and expression
- **`MetaConstraint`** — User-facing constraint DSL for building constraints declaratively
- **`LinearConstraintInput`** — Input structure for linear constraints
- **`Relation`** — Constraint relation definition (≤, =, ≥)
- **`MathInequalityDsl`** — DSL for mathematical inequality expressions
- **`MathInequalityFlatten`** — Flattening logic for inequality expressions

### Objective System

- **`Object`** — Objective function definition with category (min/max) and expression
- **`SubObject`** — Sub-objective for multi-objective optimization with priority weighting

### Auxiliary Features

- **`MechanismModelDumpSupport`** — Support for dumping mechanism models to standard form
- **`MechanismModelCutSupport`** — Cutting plane generation support
- **`MechanismModelObjectiveSupport`** — Objective function manipulation support
- **`MechanismModelFlt64Conversion`** — Type conversion to Flt64 for solver compatibility
- **`MetaModelExportSupport`** — Export models to LP/MPS file formats

## Relationships with Other Packages

- **model/basic** — Mechanism models implement base model interfaces (`Model`, `LinearModel`, etc.)
- **model/intermediate** — Mechanism models are dumped to `LinearTriadModel` / `QuadraticTetradModel`
- **token** — Models use `TokenTable` for variable and symbol management
- **symbol** — Intermediate symbols are registered and flattened through the mechanism layer