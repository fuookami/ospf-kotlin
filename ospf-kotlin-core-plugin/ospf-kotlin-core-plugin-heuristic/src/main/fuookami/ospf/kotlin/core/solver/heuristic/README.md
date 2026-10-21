# solver/heuristic — Heuristic Solver Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `heuristic` package is the contract and composition layer of the metaheuristic plugin. Every
algorithm here is **composed**, never hand-written: one lifecycle skeleton drives the loop and a set
of stateless operators decide what a single step computes.

This package also holds the public surface of the plugin — the `HeuristicSolver` contract, the
individual/population types, and 27 solver entry points.

## Package Structure

```
heuristic/                       (root package, flat on purpose)
├── HeuristicSolver.kt           # Solver contract + RunnerSolver adapter
├── HeuristicResult.kt           # HeuristicResult, HeuristicSolutionStatus
├── Iteration.kt                 # Iteration counter
├── Population.kt                # Individual, SolutionWithFitness
├── {Algorithm}.kt × 27          # One top-level function per algorithm, forwards every factory parameter
├── APSO.kt / CLPSO.kt / LSHADE.kt / GA.kt # Facades using a policy or spec data class
├── runner/                      # Lifecycle skeletons
│   ├── AbstractRunner.kt        # Loop + archive + stopping + observers, algorithm-agnostic
│   ├── IterationObserver.kt     # Reward feedback channel
│   ├── PopulationRunner.kt      # one generation = update → mutation → survivor
│   ├── TrajectoryRunner.kt      # one step = neighbourhood candidate → acceptance
│   ├── ParticleSwarmRunner.kt   # particle state (position / velocity / personal best)
│   ├── IslandRunner.kt          # N sub-populations + migration
│   ├── Nsga2Runner.kt           # True multi-objective (Pareto ranking)
│   ├── Nsga3Runner.kt           # Reference-point based MO
│   ├── ShadeRunner.kt           # Success-history memory
│   ├── ClpsoRunner.kt           # Per-dimension learning
│   └── CmaEsRunner.kt           # Distribution (m, σ²C) state
├── operator/                    # 18 operator families (see operator/README.md)
│   ├── Ordering.kt              # compareStrictly: strict-weak-ordering adapter
│   ├── NumericSupport.kt        # Bound / unwrap / clamp / entropy / probability helpers
│   ├── update/                  # SolutionUpdate · Cross · 18 updates · 10 crossovers
│   ├── mutation/                # Mutation · 12 mutations · NoMutation
│   └── rl/                      # Q-learning: table · discretizer · selector · reward
├── solver/                      # Composed solver factories
│   ├── ComposedSolvers.kt       # 34 single-objective factories
│   ├── MultiObjectiveSolvers.kt # NSGA-II / NSGA-III
│   └── MemeticSolvers.kt        # Local search attached to GA

```

### Why the facades are flat

Only the 27 facades sit directly in the root package. They carry no state and no policy of their own,
so a namespace would buy nothing. Everything that fills a slot lives under `operator/`, which keeps
the rule "a plugin comes from `operator/`" true and unqualified:

```kotlin
import fuookami.ospf.kotlin.core.solver.heuristic.*                    // contracts + 27 solvers
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.*    // SolutionUpdate, crossovers
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.*  // Mutation, mutations
```

Crossovers used to live here too, treated as "shared math primitives". That framing stopped holding
once `Mutation` was recognised as the 18th operator family — a contract plus 13 implementations,
consumed by the skeleton exactly like `SolutionUpdate`. A family belongs with the families.

There is no algorithm sub-package left. `gwo/`, `sca/`, `mvo/` and `saa/` held their own loops and
went first; `ga/` and `pso/` held pre-skeleton policy classes and went last, once `StoppingCriterion`
and the operator families covered them. `ga/`'s heterogeneous island configuration survives as
`IslandSpec(size, mutationRate)` — the old `PopulationBuilder` also carried `eliteAmount` and
`parentAmountRange`, which no skeleton reads.

## Core Concepts

### Skeleton and operator

A skeleton exposes exactly three hooks — `initialize`, `advance`, `membersOf` — plus the loop, the
archive, the stopping decision and the callback. It knows nothing about which operators it hosts.
That is why a GA crossover, a grey-wolf position update and a sine-cosine update can occupy the same
slot of `PopulationRunner`.

### Reward channel

`AbstractRunner.run` broadcasts "did this round improve" (the return value of `Archive.update`) to
every registered `IterationObserver` before advancing the iteration. Adaptive operators — Q-learning
in `operator/rl`, and any future adaptive step size — consume it instead of recomputing convergence.

### Removed legacy contracts

`Selection` / `SelectionMode` / `Normalization` / `CrossMode` / `MutationMode` are gone. They were
pre-skeleton policy parameters: selection moved to the `survivor` family, normalization to `fitness`,
migration to `operator/migration`, and the two "modes" were never read by the new skeletons at all.

## Relationships with Other Packages

- **core** — heuristic solvers use `CallBackModel` interfaces instead of sparse matrix models
- **model/callback** — provides the callback model interface for objective and constraint evaluation
- **operator/** — the 18 operator families the algorithms are composed from
- **runner/** — the lifecycle skeletons
