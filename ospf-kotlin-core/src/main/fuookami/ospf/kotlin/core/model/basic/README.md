# model/basic — Model Foundation Layer

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `basic` sub-package defines the **foundational interfaces, enums, and view types** for the OSPF model system. It establishes the type hierarchy and core abstractions used by all other model sub-packages.

## Package Structure

```
basic/
├── Model.kt                # Core model interfaces
├── ModelView.kt            # Model view interfaces
├── ExpressionRange.kt      # Expression value domain
├── ConstraintPriority.kt   # Constraint priority enum
├── ConstraintSign.kt       # Constraint sign enum
├── ObjectCategory.kt       # Objective category enum
├── MultiObject.kt          # Multi-objective support
├── ModelBuildingStage.kt   # Model building stage
├── ModelBuildingStatus.kt  # Model building status
├── ModelFileFormat.kt      # Model file format (LP, MPS, etc.)
└── RegistrationStatus.kt   # Registration status callback
```

## Core Concepts

### Model Interfaces (`Model.kt`)

Hierarchical model interfaces:

- **`Model<V>`** — Base interface: variable registration (`add`), objective setting (`addObject`, `minimize`, `maximize`), solution management (`setSolution`, `clearSolution`)
- **`LinearModel<V>`** — Linear model: linear constraints and linear objective
- **`QuadraticModel<V>`** — Quadratic model: quadratic constraints and quadratic objective
- **`ExpressionModel<V>`** — Expression model: intermediate symbol management

### Model View (`ModelView.kt`)

Read-only view interfaces for models, providing safe access without modification capabilities.

### ExpressionRange (`ExpressionRange.kt`)

Expression value domain containing:
- `lowerBound` — Lower bound
- `upperBound` — Upper bound
- `fixedValue` — Fixed value (when variable is fixed)

### Enums

- **`ConstraintPriority`** — `Mandatory`, `Suggested`, `NiceToHave`, etc.
- **`ConstraintSign`** — `LessEqual`, `Equal`, `GreaterEqual`
- **`ObjectCategory`** — `Minimum`, `Maximum`
- **`ModelBuildingStage`** — Building stages (registration, dumping, etc.)
- **`ModelFileFormat`** — `LP`, `MPS`, etc.

### Multi-Objective (`MultiObject.kt`)

Support for multi-objective optimization with priority weighting.

## Relationships with Other Packages

- **model/mechanism** — MetaModel and MechanismModel implement these interfaces
- **model/intermediate** — Triad/Tetrad models use these view types
- **token** — Model interfaces reference `TokenTable` for variable management