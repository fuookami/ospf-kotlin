# model/intermediate — Standard Form Model Layer

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `intermediate` sub-package provides the **standard form model layer** of the OSPF framework. It converts mechanism models into solver-consumable sparse matrix representations — the linear triad model (LP standard form) and quadratic tetrad model (QP standard form).

## Package Structure

```
intermediate/
├── LinearTriadModel.kt              # Linear triad model (LP standard form)
├── QuadraticTetradModel.kt          # Quadratic tetrad model (QP standard form)
├── LinearTriadDumpBuilders.kt       # Linear triad model builders
├── LinearTriadElasticBuilder.kt     # Elastic constraint builder (linear)
├── QuadraticTetradDumpBuilders.kt   # Quadratic tetrad model builders
├── QuadraticTetradElasticBuilder.kt # Elastic constraint builder (quadratic)
├── DumpHelpers.kt                   # Dump helper utilities
├── SparseMatrix.kt                  # Sparse matrix implementation
├── Cell.kt                          # Matrix cell
├── BatchDispatchPolicy.kt           # Batch dispatch policy
├── MemoryCleanupPolicy.kt           # Memory cleanup policy
├── TriadDualSolverSupport.kt        # Dual solving support
├── IntermediateModelDumpingStatus.kt # Intermediate model dumping status
└── MechanismModelDumpingStatus.kt    # Mechanism model dumping status
```

## Core Concepts

### LinearTriadModel (`LinearTriadModel.kt`)

The **linear triad model** is the standard form for linear programming:

```
min/max  c^T x
s.t.     A x {≤,=,≥} b
         l ≤ x ≤ u
```

Stores constraint coefficients as a sparse matrix `A`, objective vector `c`, right-hand side `b`, and variable bounds `l`/`u`.

### QuadraticTetradModel (`QuadraticTetradModel.kt`)

The **quadratic tetrad model** extends the triad model with quadratic terms for quadratic programming.

### Sparse Matrix (`SparseMatrix.kt`)

Efficient sparse matrix implementation for storing constraint coefficient matrices, optimized for the typical sparsity patterns in optimization models.

### Elastic Builders

- **`LinearTriadElasticBuilder`** — Automatically adds slack variables to infeasible constraints for elastic solving
- **`QuadraticTetradElasticBuilder`** — Same for quadratic models

### Dump Builders

Convert mechanism models into standard form:
- `LinearTriadDumpBuilders` — Linear mechanism model → linear triad model
- `QuadraticTetradDumpBuilders` — Quadratic mechanism model → quadratic tetrad model

### Others

- **`Cell`** — Sparse matrix cell representation
- **`BatchDispatchPolicy`** — Controls batch processing during model dumping
- **`MemoryCleanupPolicy`** — Controls memory cleanup during large model processing
- **`TriadDualSolverSupport`** — Dual variable solving support

## Relationships with Other Packages

- **model/mechanism** — Intermediate models are produced by dumping mechanism models
- **solver** — Solvers consume `LinearTriadModel` / `QuadraticTetradModel` as input
- **model/basic** — Uses `ModelView` interfaces for read-only model access