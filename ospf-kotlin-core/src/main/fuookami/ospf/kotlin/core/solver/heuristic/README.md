# solver/heuristic — Heuristic Solver Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `heuristic` sub-package provides a built-in **metaheuristic solving framework** for the OSPF framework. It includes population-based optimization algorithms such as Particle Swarm Optimization (PSO), along with supporting components for selection, crossover, mutation, migration, and iteration control.

## Package Structure

```
heuristic/
├── ParticleSwarmHeuristicSolver.kt  # Particle Swarm Optimization solver
├── Population.kt                    # Population management
├── Selection.kt                     # Selection operator
├── SelectionMode.kt                 # Selection modes (roulette, tournament, etc.)
├── Cross.kt                         # Crossover operator
├── CrossMode.kt                     # Crossover modes
├── Mutation.kt                      # Mutation operator
├── MutationMode.kt                  # Mutation modes
├── Migration.kt                     # Migration strategy between populations
├── Normalization.kt                 # Normalization utilities
├── Iteration.kt                     # Iteration control and convergence
└── Policy.kt                        # Policy definitions
```

## Core Concepts

### Particle Swarm Optimization (`ParticleSwarmHeuristicSolver.kt`)

PSO algorithm implementation that optimizes by simulating particle swarm behavior. Each particle maintains a position (solution) and velocity, guided by personal best and global best solutions.

### Population (`Population.kt`)

Manages collections of individuals (candidate solutions) for evolutionary algorithms. Supports fitness evaluation and individual lifecycle management.

### Selection Strategies (`Selection.kt`, `SelectionMode.kt`)

Selection operators for choosing parents in evolutionary algorithms:
- **Roulette** — Probability-based selection proportional to fitness
- **Tournament** — Best-of-k random selection

### Crossover Strategies (`Cross.kt`, `CrossMode.kt`)

Crossover operators combining parent solutions to produce offspring:
- Uniform crossover
- Single-point crossover
- Two-point crossover

### Mutation Strategies (`Mutation.kt`, `MutationMode.kt`)

Mutation operators introducing random perturbations:
- Uniform mutation
- Gaussian mutation
- Boundary mutation

### Migration (`Migration.kt`)

Inter-population individual migration strategy for island model parallelization.

### Iteration Control (`Iteration.kt`)

Controls iteration count, convergence criteria, and stopping conditions.

## Relationships with Other Packages

- **solver** — Heuristic solvers use `CallBackModel` interfaces instead of sparse matrix models
- **model/callback** — Provides the callback model interface for objective and constraint evaluation