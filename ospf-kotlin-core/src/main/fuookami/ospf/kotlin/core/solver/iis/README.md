# solver/iis — Infeasible Subsystem Diagnostics Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `iis` sub-package implements **Irreducible Infeasible Subsystem (IIS)** analysis for the OSPF framework. When a model is infeasible, IIS identifies the minimal subset of constraints and bounds that cause the infeasibility, aiding users in diagnosing and resolving model issues.

## Package Structure

```
iis/
├── IISComputingStatus.kt  # IIS computation status enumeration
├── IISConfig.kt           # IIS configuration
├── Linear.kt              # Linear IIS model view
└── Quadratic.kt           # Quadratic IIS model view
```

## Core Concepts

### IISConfig (`IISConfig.kt`)

Configuration for IIS computation:
- Enable/disable IIS analysis
- Control computation parameters and timeouts

### IISComputingStatus (`IISComputingStatus.kt`)

Status enumeration for IIS computation:
- Computation progress tracking
- Success/failure reporting

### Linear IIS (`Linear.kt`)

Linear infeasible subsystem model view (`BasicLinearTriadModelView`), identifying the minimal constraint subset causing linear model infeasibility.

### Quadratic IIS (`Quadratic.kt`)

Quadratic infeasible subsystem model view (`QuadraticTetradModelView`), identifying the minimal constraint subset causing quadratic model infeasibility.

## Relationships with Other Packages

- **solver/output** — IIS results are embedded in `LinearInfeasibleSolverOutput` and `QuadraticInfeasibleSolverOutput`
- **model/intermediate** — IIS views reference `LinearTriadModelView` and `QuadraticTetradModelView`