# ospf-kotlin-core-plugin-heuristic

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

Metaheuristic algorithm plugin for the OSPF Kotlin framework. Every algorithm in this module is
**composed** rather than hand-written: one lifecycle skeleton drives the loop, and a set of small
stateless operators decide what a single step computes. Adding an algorithm means choosing
operators, not writing another `while` loop.

## Architecture

Three layers, each with a single responsibility:

```
┌──────────────────────────────────────────────────────────┐
│  solver/                 组装层 / Composition             │
│    ComposedSolvers       algorithm = skeleton + operators│
│    MultiObjectiveSolvers NSGA-II / NSGA-III              │
│    MemeticSolvers        local search attached to GA     │
├──────────────────────────────────────────────────────────┤
│  runner/                 骨架层 / Skeletons               │
│    AbstractRunner        iteration · archive · callback   │
│    PopulationRunner      cross → mutate → survive         │
│    TrajectoryRunner      neighbour → accept               │
│    ParticleSwarmRunner   velocity + personal best state   │
│    IslandRunner          N sub-populations + migration    │
│    Nsga2Runner           true multi-objective ranking     │
│    Nsga3Runner           reference-point based MO         │
│    ShadeRunner           success-history memory           │
│    ClpsoRunner           per-dimension learning           │
│    CmaEsRunner           distribution (m, σ²C) state      │
├──────────────────────────────────────────────────────────┤
│  operator/               算子层 / Operators (17 families) │
│    update · mutation · survivor · topology · initializer  │
│    fitness · ranking · boundary · stopping · archive      │
│    diversity · neighborhood · stochastic · evolution      │
│    acceptance · multi_objective · migration · rl          │
├──────────────────────────────────────────────────────────┤
│  crossovers + mutations  (root) 数学实现 / Math impls      │
└──────────────────────────────────────────────────────────┘
```

The skeleton owns the loop, the archive, the stopping decision and the callback. It knows nothing
about which operators it hosts. This is why `GreyWolfUpdate`, `ArithmeticCross` and `PpfUpdate` can
all occupy the same slot of `PopulationRunner` without the skeleton containing a single branch for
any of them.

### Two-level step template

A skeleton exposes exactly three hooks:

| Hook | Meaning |
|------|---------|
| `initialize(model, iteration)` | Build the initial state |
| `advance(model, iteration, state)` | Produce the next state |
| `membersOf(state)` | Flatten the state into individuals |

`PopulationRunner` adds a second level: `openIsland` / `stepIsland`. `IslandRunner` drives N
`PopulationRunner`s through those two public wrappers, so the island model reuses the same
population loop instead of duplicating it.

## Algorithms

All of the following are available through `ComposedSolvers` and are pure compositions — no
algorithm-specific loop code.

| Solver name | Factory | Family |
|-------------|---------|--------|
| `genetic` | `genetic` | GA |
| `island-ga` | `islandGenetic` | GA (island model) |
| `pso` | `particleSwarm` | PSO |
| `apso` | `adaptiveParticleSwarm` | PSO variant |
| `clpso` | `comprehensiveLearningParticleSwarm` | PSO variant |
| `gwo` | `greyWolf` | swarm |
| `sca` | `sineCosine` | swarm |
| `mvo` | `multiVerse` | swarm |
| `hs` | `harmonySearch` | swarm |
| `wca` | `waterCycle` | swarm |
| `soa` | `seagull` | swarm |
| `ns` | `neighborhoodSearch` | swarm |
| `warso` | `warStrategy` | swarm |
| `evo` | `energyValley` | swarm |
| `gco` | `germinalCenter` | swarm |
| `de` | `differentialEvolution` | evolution |
| `woa` | `whaleOptimization` | swarm |
| `hho` | `harrisHawk` | swarm |
| `aoa` | `arithmeticOptimization` | swarm |
| `tlbo` | `teachingLearning` | swarm |
| `ssa` | `salpSwarm` | swarm |
| `gsa` | `gravitationalSearch` | swarm |
| `fa` | `firefly` | swarm |
| `cs` | `cuckooSearch` | swarm |
| `ba` | `batAlgorithm` | swarm |
| `mfo` | `mothFlame` | swarm |
| `goa` | `grasshopper` | swarm |
| `shade` | `shade` | advanced |
| `l-shade` | `lShade` | advanced |
| `cma-es` | `cmaEs` | advanced |
| `q-learning` | `qLearning` | adaptive (operator selection) |
| `simulated-annealing` | `simulatedAnnealing` | trajectory |
| `hill-climbing` | `hillClimbing` | trajectory |
| `random-walk` | `randomWalk` | trajectory |

True multi-objective solvers live in `MultiObjectiveSolvers` (`Nsga2Runner`, `Nsga3Runner`) and
operate on Pareto fronts rather than a single objective value. Memetic variants live in
`MemeticSolvers`.

## File Structure

```
heuristic/                          (the plugin root package)
├── contracts
│   ├── HeuristicSolver.kt            solver contract + RunnerSolver adapter
│   ├── HeuristicResult.kt            HeuristicResult, HeuristicSolutionStatus
│   ├── Iteration.kt                  iteration counter
│   └── Population.kt                 Individual, SolutionWithFitness
├── algorithm facades (root)          27 top-level functions + 4 policy/spec data classes
├── runner/                           lifecycle skeletons
├── operator/                         18 operator families (see operator/README.md)
│   ├── Ordering.kt                   compareStrictly: strict-weak-ordering adapter
│   ├── NumericSupport.kt             bound / unwrap / clamp / entropy / probability helpers
│   ├── update/                       SolutionUpdate · Cross · 18 algorithm updates
│   │                                 · SinglePoint · TwoPoint · MultiPoint · Uniform · Arithmetic
│   │                                 · Blend · SBX · Differential · Order · Cycle
│   ├── mutation/                     Mutation · Gaussian · Cauchy · Levy · Uniform · Boundary
│   │                                 · Polynomial · NonUniform · Chaotic · Swap · Inversion
│   │                                 · Insertion · Scramble · NoMutation
│   ├── migration/                    IslandMigration: No / Ring / BestToWorst
│   └── rl/                           QTable · StateDiscretizer · ActionSelector · Reward
└── solver/                           ComposedSolvers · MultiObjectiveSolvers · MemeticSolvers
```

Only the facades sit directly in the root package: a facade carries no state and no policy of its own,
so it needs no namespace. Everything that fills a slot — crossovers, mutations, survivors,
initializers — lives under `operator/`, which keeps the rule "a plugin comes from `operator/`" true:

```kotlin
import fuookami.ospf.kotlin.core.solver.heuristic.*                    // contracts + 27 solvers
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.*    // crossovers
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.*  // mutations
```

`Cross` extends `SolutionUpdate`, so the `update` family owns it together with the crossovers — one
slot, one package. `Mutation` is the sibling slot and therefore the 18th family, which is why there is
an `operator/mutation/` at all.

### Facades forward everything

A facade carries **every** parameter of the factory it delegates to, so `differentialEvolution(...)`
and `ComposedSolvers.differentialEvolution(...)` are interchangeable apart from the prefix. When the
optional-parameter count reaches five, the knobs are grouped into a policy data class instead of being
spread over the signature:

| Facade | Optional parameters | Shape |
|--------|--------------------|-------|
| 18 population facades | 4 | `update`, `survivor`, `archiveAmount`, `initializer` inline |
| `hillClimbing`, `neighborhoodSearch` | 2 | `neighborhood`, `initializer` inline |
| `cmaEs` | 2 | `initialSigma`, `archiveAmount` inline |
| `shade` | 3 | `memorySize`, `pBestRate`, `archiveAmount` inline |
| `lShade` | 5 | `policy: LShadePolicy` |
| `adaptiveParticleSwarm` | 6 | `policy: AdaptiveParticleSwarmPolicy` |
| `comprehensiveLearningParticleSwarm` | 7 | `policy: ComprehensiveLearningParticleSwarmPolicy` |
| `particleSwarm` | 6 | `inertia`, `personalWeight`, `socialWeight`, `maxVelocity`, `archiveAmount`, `initializer` inline |
| `islandGenetic` | 7 | `mutation`, `migrationPeriod`, `migration`, `update`, `survivor`, `archiveAmount`, `initializer` inline |

A policy is used when the tail is **numeric hyper-parameters** — SHADE's memory size and p-best rate,
APSO/CLPSO's weight schedules — because a caller tunes those as a group and their meaning only exists
inside one algorithm. A flat tail is used when the tail is **operator slots** (`particleSwarm`'s three
weights, `islandGenetic`'s migration pair plus the standard population slots), because those are meant
to be plugged and the 1:1 correspondence with the factory is the point. The five-parameter threshold is
a guideline, not an invariant.

Either way a policy bundles only algorithm knobs: `stopping`, `converter` and `entropy` stay explicit,
because `StoppingCriterion` is composable (`and` / `or`) and a policy-shaped limit would lose that.

`FacadeContractTest` pins the contract at compile time: every facade is called with all of its
parameters spelled out, so dropping one from a signature breaks the build.

No algorithm package is left. `gwo/`, `sca/`, `mvo/` and `saa/` carried their own loops and were the
first to go; `ga/` and `pso/` kept policy classes shaped for the pre-skeleton world (`GAPolicy`,
`PSOPolicy` with `iterationLimit` / `timeLimit` inside) and were folded last, once the skeleton's
`StoppingCriterion` and operators covered them. Their entry points are now the `islandGenetic` and
`particleSwarm` facades.

The island model needed one new type: `IslandSpec(size, mutationRate)`. It replaces the old
`PopulationBuilder`, which also carried `eliteAmount` and `parentAmountRange` — fields the skeleton
never read, so they were dropped rather than migrated.

## Common Features

- **Generic value types** — works with any `RealNumber<V>` + `NumberField<V>`
- **Single- and multi-objective** — single-objective solvers share one archive; multi-objective ones
  use Pareto-based operators
- **Iteration and time limits** — composable stopping criteria (`and` / `or`)
- **Running callbacks** — observe progress without owning the loop
- **Adaptive parameters** — `operator/evolution` combines a `ParameterSource` with the
  `chaotic` / `fractal` / `distribution` generators from `ospf-kotlin-math`
- **Memory pressure handling** — automatic cleanup between iterations

## Usage

### Dependency

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-heuristic:1.1.0")
```

### Composed solvers

```kotlin
val solver = ComposedSolvers.greyWolf<Flt64, Flt64>(
    size = 30,
    stopping = IterationLimit(UInt64(500)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() }
)
val result = solver(model)   // Ret<HeuristicResult<...>>
```

### Genetic algorithm (island model)

Each `IslandSpec` is one island, so islands may differ in size and mutation rate. Islands evolve
independently and exchange individuals every `migrationPeriod` generations.

```kotlin
val solver = islandGenetic<Flt64, Flt64>(
    islands = listOf(
        IslandSpec(size = 20, mutationRate = Flt64(0.05)),
        IslandSpec(size = 30, mutationRate = Flt64(0.15))
    ),
    stopping = IterationLimit(UInt64(300)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    migrationPeriod = UInt64(5),
    migration = RingMigration(rate = 0.1)   // or BestToWorstMigration(1), NoMigration()
)
val result = solver(model)
```

`ComposedSolvers.islandGenetic` also keeps a uniform overload (`islandCount` / `islandSize` /
`mutationRate`) for the common case; the facade uses the heterogeneous one.

### Particle swarm optimization

```kotlin
val solver = particleSwarm<Flt64, Flt64>(
    size = 100,
    stopping = IterationLimit(UInt64(500)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    inertia = ConstantSource(Flt64(0.4)),
    personalWeight = ConstantSource(Flt64(2.0)),
    socialWeight = ConstantSource(Flt64(2.0))
)
val result = solver(model)
```

The three weights accept any `ParameterSource`, so a time-varying variant is a different schedule on
the same code — or use the `adaptiveParticleSwarm` facade, which wires that up.

### Simulated annealing

```kotlin
val saa = ComposedSolvers.simulatedAnnealing<Flt64, Flt64>(
    stopping = IterationLimit(UInt64(500)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    temperature = GeometricSource(Flt64(100.0), Flt64(0.95))
)
val result = saa(model)
```

### Q-learning

Q-learning adapts the search itself. It ships as an operator family (`operator/rl/`) with two
uses: **parameter tuning** and **operator selection**.

```kotlin
// Operator selection: one Q-table over a pool of candidate updates.
val solver = ComposedSolvers.qLearning<Flt64, Flt64>(
    size = 30,
    stopping = IterationLimit(UInt64(400)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    candidates = listOf(GreyWolfUpdate(), SineCosineUpdate(), ArithmeticCross),
    selector = EpsilonGreedyActionSelector(epsilon = Flt64(0.1)),
    reward = ImprovementReward(positive = Flt64.one, negative = Flt64(-0.5))
)

// Parameter tuning: one Q-table over a pool of candidate schedules.
val inertia = QlearningSource(
    candidates = listOf(ConstantSource(Flt64(0.4)), LinearSource(Flt64(0.9), Flt64(0.4))),
    randomGenerator = Generator { Random.nextFlt64() }
)
```

The reward is the archive's real improvement (`Archive.update`'s return value), delivered to
observers by the skeleton after every round — see `runner/IterationObserver.kt`. The state is the
pair *(diversity, stagnation)*, so no objective value is needed to build the table.

### Custom skeleton

To add an algorithm whose loop genuinely differs, extend `AbstractRunner` and implement the three
hooks — do not copy an existing loop:

```kotlin
class MyRunner(...) : AbstractRunner<MyState, ObjValue, V>(entropy, archive, stopping, converter) {
    override suspend fun initialize(model: ..., iteration: Iteration): MyState = ...
    override suspend fun advance(model: ..., iteration: Iteration, state: MyState): MyState = ...
    override fun membersOf(state: MyState): List<Individual<ObjValue, V>> = ...
}
```

## Dependencies

| Dependency | Scope | Description |
|------------|-------|-------------|
| `ospf-kotlin-core` | compile | Model interfaces, `Individual`, `Ret` |
| `ospf-kotlin-math` | compile | `generator`, `chaotic`, `fractal`, `distribution` |
