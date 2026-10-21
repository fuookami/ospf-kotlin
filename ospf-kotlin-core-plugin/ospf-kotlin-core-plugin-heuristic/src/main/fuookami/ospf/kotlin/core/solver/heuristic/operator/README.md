# operator — Algorithm Operator Families

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Metaheuristics share a small number of reusable decisions. This package names them as orthogonal operator families so an algorithm becomes a **composition** of operators rather than a hand-written loop.

Every family is an interface with one or more interchangeable implementations. Interfaces whose operation does not carry state declare their type parameters on the method (so implementations can be `data object` singletons); stateful families (`Archive`) declare them on the type.

## Families

| Family | Interface | Implementations | Purpose |
|--------|-----------|-----------------|---------|
| Initialization | `initializer/Initializer` | `ModelInitializer`, `RandomUniformInitializer`, `OppositionBasedInitializer` | Produce the initial solutions |
| Fitness | `fitness/FitnessEvaluator` | `ObjectiveFitnessEvaluator`, `DefaultOnMissFitnessEvaluator`, `ConstraintAwareFitnessEvaluator` | Score a solution, optionally rejecting infeasible ones |
| Ranking | `ranking/Ranking` | `ObjectiveRanking`, `ReversedObjectiveRanking`, `IdentityRanking` | Order individuals by quality |
| Boundary | `boundary/BoundaryHandler` | `ClampBoundary`, `ReflectBoundary`, `WrapBoundary` | Bring out-of-range values back into the feasible region |
| Stopping | `stopping/StoppingCriterion` | `IterationLimit`, `NotBetterLimit`, `TimeLimit`, `NeverStop` (+ `or` / `and`) | Decide when to stop |
| Topology | `topology/Topology` | `GlobalTopology`, `RingTopology`, `RingKTopology`, `VonNeumannTopology`, `RandomTopology` | Define neighbour relations |
| Archive | `archive/Archive` | `BestArchive`, `HistoryBestArchive` | Track the best set found so far |
| Diversity | `diversity/DiversityMetric` | `MeanPairwiseDistance`, `FractalDimensionDiversity` | Detect population collapse |
| Survivor | `survivor/SurvivorSelection` | `ElitistSurvivor`, `GenerationalSurvivor`, `TruncationSurvivor`, `ElitistPreservingSurvivor` | Merge parents and offspring |
| Neighbourhood | `neighborhood/Neighborhood` | `RandomPerturbation`, `GaussianPerturbation`, `LevyPerturbation` | Generate a neighbour of a single solution |
| Stochastic | `stochastic/` | `LevyFlight`, `NoiseStep` | Multi-dimensional random steps |
| Parameter evolution | `evolution/ParameterSource` | `ConstantSource`, `LinearSource`, `GeometricSource`, `ExponentialSource`, `ChaoticSource`, `FractalSource`, `DistributionSource`, `JitteredSource` | Make algorithm parameters vary with iteration |
| Solution update | `update/SolutionUpdate` | `Cross` + 10 crossovers (`SinglePointCross`, `ArithmeticCross`, `SimulatedBinaryCross`, `DifferentialCross`, `OrderCross`, `CycleCross`, …) plus per-algorithm updates (`GreyWolfUpdate`, `PpfUpdate`, …) | Compute the next solution/position from the parent group |
| Mutation | `mutation/Mutation` | `GaussianMutation`, `CauchyMutation`, `LevyMutation`, `UniformMutation`, `BoundaryMutation`, `PolynomialMutation`, `NonUniformMutation`, `ChaoticMutation`, `SwapMutation`, `InversionMutation`, `InsertionMutation`, `ScrambleMutation`, `NoMutation` | Perturb an offspring after the update step; `NoMutation` occupies the slot for algorithms that do not mutate |
| Acceptance | `acceptance/Acceptance` | `BetterOnlyAcceptance`, `NotWorseAcceptance`, `AlwaysAcceptance`, `MetropolisAcceptance` | Decide whether a candidate replaces the current solution |
| Multi-objective | `multi_objective/` | `ParetoDominance`, `FastNonDominatedSort`, `CrowdingDistance`, `ReferencePoints`, `ParetoArchive` | True Pareto-based selection |
| Migration | `migration/IslandMigration` | `NoMigration`, `RingMigration`, `BestToWorstMigration` | Exchange individuals between islands |
| Reinforcement learning | `rl/` | `QTable`, `DiversityStagnationDiscretizer`, `GreedyActionSelector`, `EpsilonGreedyActionSelector`, `SoftmaxActionSelector`, `ImprovementReward`, `SymmetricReward`, `QlearningAgent`, `QlearningSource`, `QlearningUpdate` | Adapt parameters or select operators by Q-learning |

`Cross` extends `SolutionUpdate`, so it lives in `update/` with the crossovers — one slot, one family.
`Mutation` is the sibling slot and therefore its own family in `mutation/`. Both used to sit in the
plugin root package as "shared math primitives", which stopped being true once the skeleton started
consuming `Mutation` through a first-class slot.

## Composition Example

```kotlin
// 混沌序列 → 参数区间 → 抖动 → 惯性权重
val inertia = JitteredSource(
    base = ChaoticSource(generator = LogisticMapGenerator(), lowerBound = Flt64(0.4), upperBound = Flt64(0.9)),
    entropy = entropy,
    amplitude = Flt64(0.02)
)

// 分形序列（Mandelbrot 迭代）→ 幅值投影 → 步长
val step = FractalSource(
    generator = MandelbrotSetGenerator(),
    projection = ChannelProjection.Magnitude,
    lowerBound = Flt64(0.01),
    upperBound = Flt64(0.5)
)

// 种群多样性 → 自适应步长
val diversity = FractalDimensionDiversity(solutions, converter)
val adaptive = if (diversity.toDouble() < 1.2) Flt64(0.5) else Flt64(0.05)
```

## Strict Ordering

`compareStrictly(model, lhs, rhs)` normalises the model objective comparison into a strict weak ordering. Models may register non-strict comparators, in which case `compareObjective(a, a)` returns `Order.Less`; every operator here uses `compareStrictly` so ties stay ties, sorting stays contract-compliant, and "has improved" detection does not fire on equal objectives.

## Numeric Support

`NumericSupport.kt` holds the `internal` boilerplate every operator and skeleton would otherwise
duplicate: `lowerBoundOf` / `upperBoundOf` (read a variable's box bounds from the callback model),
`asDouble` / `asValue` (unwrap a numeric solution value and put it back), `unit()` (sample a uniform
`[0, 1)` number from a `Generator<Flt64>`), `pickTwo`, `clampTo` and `shouldFire` (fire an event at a
given probability).

`clampTo` is the one that matters most: besides clamping it replaces `NaN` / `±∞` with an available
bound. A non-finite value that reaches a population overflows the objective to `∞`, and comparisons
between `∞` values do not form a total order — the survivor sort then throws
`Comparison method violates its general contract`.

The file was called `GeneSupport.kt` and lived in the plugin root until the chromosome concept was
removed; nothing in it was ever gene-specific.

## Related

- [Heuristic Plugin README](../../README.md)
- [Distribution Module](../../../../../../../../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/distribution/README.md)
- [Sequence Projection](../../../../../../../../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/sequence/ChannelProjection.kt)
