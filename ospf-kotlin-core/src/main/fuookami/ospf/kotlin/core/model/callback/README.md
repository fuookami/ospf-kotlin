# model/callback — Callback Model Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `callback` sub-package provides **callback model interfaces** for heuristic and metaheuristic solvers in the OSPF framework. Unlike standard solvers that consume sparse matrix models, heuristic solvers evaluate objectives and constraints through callback functions.

## Package Structure

```
callback/
├── CallBackModelInterface.kt  # Callback model interface definition
└── CallBackModel.kt           # Callback model implementation
```

## Core Concepts

### CallBackModelInterface (`CallBackModelInterface.kt`)

Interface definition for callback-based model evaluation. Heuristic solvers use this interface to:
- Evaluate objective function values for candidate solutions
- Evaluate constraint violations
- Query variable bounds and types

### CallBackModel (`CallBackModel.kt`)

Implementation of the callback model interface, bridging the standard model representation with the callback-based evaluation paradigm used by heuristic solvers.

## Design Philosophy

Standard solvers (Gurobi, CPLEX, SCIP) operate on sparse matrix representations (`LinearTriadModel` / `QuadraticTetradModel`). Heuristic solvers, however, need a different interface because they:
- Generate candidate solutions as variable assignments
- Need to evaluate objectives and constraints for each candidate
- May not benefit from matrix representations

The callback model bridges this gap by providing a functional evaluation interface.

## Relationships with Other Packages

- **solver/heuristic** — Heuristic solvers consume `CallBackModel` for objective and constraint evaluation
- **model/basic** — Callback models implement base model interfaces
- **token** — Callback models use `TokenTable` to access variable solve results