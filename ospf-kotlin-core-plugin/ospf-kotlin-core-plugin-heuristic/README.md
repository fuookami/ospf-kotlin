# ospf-kotlin-core-plugin-heuristic

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

Metaheuristic algorithm plugin for the OSPF Kotlin framework. This module provides a collection of population-based and trajectory-based metaheuristic optimization algorithms, all following a unified policy-driven architecture.

## Algorithms

| Algorithm | Status | Type | Description |
|-----------|--------|------|-------------|
| GA | :white_check_mark: | Population | Genetic Algorithm — selection, crossover, mutation, migration |
| PSO | :white_check_mark: | Population | Particle Swarm Optimization — inertia weight, acceleration |
| GWO | :white_check_mark: | Population | Grey Wolf Optimizer — alpha/beta/delta leadership hierarchy |
| SCA | :white_check_mark: | Population | Sine Cosine Algorithm — sine-cosine position update with Q-learning |
| SAA | :white_check_mark: | Trajectory | Simulated Annealing — temperature scheduling, Metropolis criterion |
| MVO | :white_check_mark: | Population | Multi-Verse Optimizer — white hole/wormhole traversal |
| EVO | :construction: | — | Energy Valley Optimizer (placeholder) |
| GCO | :construction: | — | Germinal Center Optimization (placeholder) |
| HCA | :construction: | — | Hill Climbing Algorithm (placeholder) |
| HS | :construction: | — | Harmony Search (placeholder) |
| NS | :construction: | — | Neighborhood Search (placeholder) |
| SOA | :construction: | — | Seagull Optimization Algorithm (placeholder) |
| WarSO | :construction: | — | War Strategy Optimization Algorithm (placeholder) |
| WCA | :construction: | — | Water Cycle Algorithm (placeholder) |

## Architecture

All algorithms follow a **policy-driven** design:

```
┌──────────────────────────────────────────────────┐
│  {Algorithm} (e.g., GeneAlgorithm, PSO, GWO)    │  Main runner
│    ├─ policy: Abstract{Algorithm}Policy          │  Strategy interface
│    ├─ invoke(model, runningCallBack)             │  Execute
│    └─ Iteration tracking, good solution pool     │
├──────────────────────────────────────────────────┤
│  {Algorithm}Policy                               │  Concrete strategy
│    ├─ HeuristicPolicy (base)                     │  Iteration/time limits
│    └─ AbstractHeuristicPolicy                    │  coerceIn, update, finished
├──────────────────────────────────────────────────┤
│  Individual types                                │
│    ├─ GA: Chromosome / Population                │
│    ├─ PSO: Particle                              │
│    ├─ GWO: Wolf / Population                     │
│    ├─ SCA/SAA/MVO: SolutionWithFitness           │
│    └─ All implement Individual<ObjValue, V>       │
└──────────────────────────────────────────────────┘
```

## File Structure

```
heuristic/
├── ga/         Genetic Algorithm
│   ├── GA.kt           GeneAlgorithm, GAPolicy, AbstractGAPolicy
│   └── Population.kt   Chromosome, Population type aliases
├── pso/        Particle Swarm Optimization
│   ├── PSO.kt          ParticleSwarmOptimizationAlgorithm, PSOPolicy
│   └── Particle.kt     Particle data class
├── gwo/        Grey Wolf Optimizer
│   ├── GWO.kt          GreyWolfOptimizer, GWOPolicy
│   └── Population.kt   Wolf, alpha/beta/delta helpers
├── sca/        Sine Cosine Algorithm
│   └── SCA.kt          SineCosineAlgorithm, SCAPolicy, QLearningState
├── saa/        Simulated Annealing
│   └── SAA.kt          SimulatedAnnealingAlgorithm, SAAPolicy
├── mvo/        Multi-Verse Optimizer
│   └── MVO.kt          MultiVerseOptimizer, MVOPolicy
├── evo/        Energy Valley Optimizer (placeholder)
├── gco/        Germinal Center Optimization (placeholder)
├── hca/        Hill Climbing Algorithm (placeholder)
├── hs/         Harmony Search (placeholder)
├── ns/         Neighborhood Search (placeholder)
├── soa/        Seagull Optimization Algorithm (placeholder)
├── warso/      War Strategy Optimization Algorithm (placeholder)
└── wca/        Water Cycle Algorithm (placeholder)
```

## Common Features

All implemented algorithms support:

- **Generic value types** — works with any `RealNumber<V>` + `NumberField<V>`
- **Single- and multi-objective** — via type aliases (`GA` / `MulObjGA`, `PSO` / `MulObjPSO`, etc.)
- **Iteration and time limits** — configurable stopping criteria
- **Running callbacks** — observe progress at each iteration
- **Memory pressure handling** — automatic cleanup between iterations
- **Good solution pool** — maintains sorted list of best solutions found

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-heuristic:1.1.0")
```

### Genetic Algorithm

```kotlin
val ga = GA(
    population = listOf(
        PopulationBuilder(
            eliteAmount = UInt64(2),
            densityRange = ValueRange(UInt64(20), UInt64(50)),
            mutationRateRange = ValueRange(Flt64(0.01), Flt64(0.1)),
            parentAmountRange = ValueRange(UInt64(2), UInt64(4))
        )
    ),
    migrationPeriod = UInt64(10),
    policy = GAPolicy(...)
)
val bestSolutions = ga(model)
```

### Particle Swarm Optimization

```kotlin
val pso = PSO(
    particleAmount = UInt64(100),
    solutionAmount = UInt64.one,
    policy = PSOPolicy(w = Flt64(0.4), c1 = Flt64(2.0), c2 = Flt64(2.0))
)
val bestSolutions = pso(model)
```

### Simulated Annealing

```kotlin
val saa = SimulatedAnnealingAlgorithm(
    policy = SAAPolicy(
        initialTemperature = Flt64(100.0),
        finalTemperature = Flt64(1.0),
        temperatureGradiant = Flt64(0.98)
    )
)
val bestSolutions = saa(model)
```

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Heuristic policy interfaces, Individual, Iteration |
