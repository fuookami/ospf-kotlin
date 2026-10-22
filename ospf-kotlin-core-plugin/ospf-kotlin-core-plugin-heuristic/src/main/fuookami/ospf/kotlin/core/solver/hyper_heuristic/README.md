# Hyper-Heuristic Search

English | [简体中文](README_ch.md)

This package is part of **ospf-kotlin-core-plugin-heuristic**. It adds no Maven artifact.
Dependency direction is `hyper_heuristic -> heuristic`: algorithm runners do not depend on high-level policies.

## Structure

| Package | Responsibility |
| --- | --- |
| root | Configuration, solver, isolated run, composition facade |
| `selection` | Algorithm/operator policies, masks, typed operator bindings |
| `scheduling` | Slice budgets, composite sessions and scheduling |
| `memory` | Valid archive, accepted working set, acceptance and exchange |
| `learning` | Features, rewards, instance training, evaluation and trajectory replay |
| `configuration` | Kotlin DSL, JSON/YAML, registry and capability validation |
| `persistence` | Versioned policies/checkpoints and atomic local-file storage SPI |

## Registration And Configuration

Create a `HyperHeuristics.registry<O, V>(converter)` and register a problem factory using
`registerProblem(id, statelessCallbacks, factory)`. Each invocation must construct a **new model,
token table and callback/cache state**. Wrap existing models with `singleObjectiveProblem`, or use
`multiObjectiveProblem` and `HyperHeuristics.multiObjectiveRegistry` for vector objectives.
Problem fingerprints must identify input data, encoding, objective directions and callback semantics.
The registry never instantiates arbitrary class names or serializes closures.

```kotlin
val configured = hyperHeuristic {
    problem = ComponentSpec("my-problem")
    budget = BudgetConfig(evaluations = 10_000, sliceEvaluations = 256)
    execution = ExecutionConfig(
        mode = ExecutionMode.Synchronous,
        parallelism = 2,
        seed = 42
    )
    selection = ComponentSpec("ucb1")
    algorithm(id = "swarm", type = "pso") { size = 20 }
    algorithm(id = "adaptive-ga", type = "genetic") {
        size = 20
        updates = listOf(ComponentSpec("gwo"), ComponentSpec("sca"))
        mutations = listOf(ComponentSpec("gaussian"), ComponentSpec("none"))
        selection = ComponentSpec("q-learning")
    }
    persistence = PersistenceConfig(enabled = true)
}
// 先处理 Ret 失败 / Handle Ret failures before constructing the solver.
```

`ConfigurationFormats.encode/decode` reads the **same configuration model** as JSON or YAML.
For example, this YAML needs only a registered `my-problem` factory:

```yaml
version: 1
problem: {type: my-problem}
algorithms:
  - id: swarm
    algorithm: {type: pso}
    size: 20
  - id: adaptive-ga
    algorithm: {type: genetic}
    size: 20
    updates: [{type: gwo}, {type: sca}]
    mutations: [{type: gaussian}, {type: none}]
    selection: {type: q-learning}
budget: {evaluations: 10000, sliceEvaluations: 256}
selection: {type: ucb1}
execution: {mode: Synchronous, parallelism: 2, seed: 42}
persistence: {enabled: true}
```

Component references contain `type`, `version` and `parameters`; business inputs belong in the
registered problem's `parameters`. Algorithms use stable instance IDs (letters, numbers, `.`, `_`, `-`).
Defaults are synchronous execution, round-robin selection, fixed slices and non-worsening acceptance.
At least one global evaluation, decision, low-level step or time limit is required.
Unknown components, parameters, versions, slots and incompatible encodings return `Ret` failures.

## Algorithms And Operator Slots

Built-in scalar IDs:

- `genetic`, `gwo`, `sca`, `mvo`, `hs`, `wca`, `soa`, `warso`, `evo`, `gco`, `de`, `woa`,
  `hho`, `aoa`, `tlbo`, `ssa`, `gsa`, `fa`, `cs`, `ba`, `mfo`, `goa`.
- `pso`, `apso`, `clpso`, `shade`, `l-shade`, `cma-es`.
- `hill-climbing`, `ns`, `random-walk`, `simulated-annealing`.
- `nsga-ii` and `nsga-iii` are registered by the multiobjective facade.
- `island` / `island-ga` alternate persistent `children`; `migrationPeriod` controls migration.
  `memetic` advances through configured child stages; `phaseSteps` controls stage length.
  Children share the parent evaluation allowance and retain their own random streams.

Population algorithms and NSGA expose update/mutation slots; trajectory algorithms expose neighborhoods.
PSO exposes `inertia`, `personalWeight`, `socialWeight`; CLPSO exposes `inertia`;
L-SHADE exposes `sizeRatio`; simulated annealing exposes `temperature` parameter sources.
Only actual supported slots are accepted. `genetic` plus explicitly compatible operators supports
permutation/custom encodings; incompatible continuous algorithms are rejected.

Selectors: `fixed` (fixed cyclic sequence), `round-robin`, `stagnation`, `random`, `epsilon-greedy`,
`ucb1`, `q-learning`. Learning parameters are `epsilon`, `alpha`, `gamma`, `states` and `stagnationLimit`.
The built-in progress feature space requires at least 64 states. Frozen mode disables parameter learning
and learning-policy exploration, not the scheduling cursor needed by rule policies.

For multiobjective search, scalar algorithms **must** refer to an explicitly registered scalarization
via `AlgorithmConfig.scalarization`. There is no implicit sum and `SearchResult.best` is null.
The valid archive keeps canonical objective vectors even when a scalar algorithm generated the candidate.

## Sessions, Budgets And Execution

`AbstractRunner.run` now returns `Ret<List<Individual<O, V>>>`; unwrap/propagate the result at callers.
`RunnerSolver` and the NSGA facade propagate failures. `openSession(problem, codec, importer)` and
complete solving drive the same initialize/advance hooks, without repeated full solver calls.
`RunnerStateCodecs` preserves populations, nested islands, particles, CLPSO exemplars, SHADE memory,
and CMA-ES covariance/evolution paths. Create a separate runner per simultaneously live session.

A slice charges initialization, imports and repeated/cached evaluation requests against atomic global
tokens. `SearchResult.evaluations` counts requests and `objectiveCalls` counts admitted model objective
invocations after feasibility checks (not individual objective terms or work hidden inside callbacks).
Custom sessions should evaluate through `SearchProblem.evaluate(solution, budget)`.
Budgets count decisions, complete low-level steps, evaluations and active time separately.
Incomplete generations do not replace native state; completed candidate evaluations remain available.
Time limits are cooperative and cannot preempt a blocking business callback.

The coordinator alone mutates global memory and high-level learning state. Workers own independent
models and random streams. Inner runner tasks execute serially within a slice, bounding total concurrency
by the outer scheduler. Synchronous batches merge in stable instance-ID order; asynchronous completion
order is intentionally nondeterministic. Dispatch context, decision ID, actual action and policy revision
travel with feedback; unrelated later actions are never credited.

`solver.open()` returns an isolated `HyperHeuristicRun`; use `advance`, `run`, `requestStop`, `result`,
`transitions`, `exportPolicy`, `checkpoint` and `close`. The complete `solver()` entry owns cleanup.
When managing runs explicitly, always close them in `finally`. Stop/checkpoint/close drain in-flight
slices rather than discarding feedback. Root transitions use scope `algorithm`; operator scopes use
stable paths such as `adaptive-ga/update` or `islands/child-a/mutation`.

Global valid archives and accepted working sets are separate. A high-level rejection does not undo
private particle/history/distribution state. Only validated, explicitly feasible candidates are archived.
`initialization = WorkingSet` imports available working solutions after the first complete native
initialization; imports are re-evaluated and charged. `importMode = Disabled` disallows external imports.
Periodic exchange uses `ComponentSpec("periodic", parameters)` with positive `period` and `amount`.
CMA-ES needs `restartOnImport: true`, which explicitly restarts its distribution around imported seeds.

## Training, Replay And Persistence

`PolicyTrainer.train(config, TrainingSplit(...), seeds, initial)` trains across **disjoint problem
instances**, creating new sessions per episode, then evaluates validation/test instances in frozen mode.
`PolicyEvaluator.evaluate` runs a configured fixed algorithm, random, round-robin or learned policy
under the same explicit evaluation budget; observations retain raw objective archives and cost so callers
can apply domain-specific quality metrics. `PolicyEvaluator.compare` additionally reports quality samples,
aggregate cost and failure rates for named baselines under identical evaluation allowances. `PolicyTrainer.replay` takes an explicit trajectory scope;
Q-learning performs masked off-policy terminal updates, while Bandit observes only executed actions.
`TrajectoryFiles` provides atomic JSON Lines writing and full-record reading. Set `trajectoryPath` to
write completed runs. No external ML platform is required.

Use `FileSearchStore(directory)` to save/load `PolicyArchive` and `RunCheckpoint` by safe local keys.
Policies store learned tables/statistics and component definitions; checkpoints additionally store all
native states, operator policies, archives, working sets, budget/time, random streams, decision order,
problem callback state and completed trajectories. Save operations stop dispatching, drain, merge and
observe before creating a quiescent snapshot. Paused/storage time does not count as active runtime.
Atomic replacement and payload checksums prevent partial documents from being accepted.

Recovery is explicit: load a document, then call `solver.open(checkpoint = saved)` or
`solver.open(policy = savedPolicy)`; do not supply both. Checkpoints require identical configuration
and problem fingerprints. Policies align actions by stable ID. Configuration, policy and checkpoint
versions are independently checked. Supply explicit `ConfigurationMigration` maps to format decoding
or file stores for supported old document versions; unknown versions are rejected.

With persistence enabled, problem factories must declare stateless callbacks or provide
`SearchProblemState`. Stateful custom operators must implement `RecoverableComponent`; explicitly
stateless operator registrations need no state codec. Custom selectors own their snapshot/restore
protocol. Feature/reward/acceptance/exchange/allocator registrations must also explicitly declare statelessness
or implement `RecoverableComponent`; stateful instances participate in full checkpoint recovery.
Unrecoverable components fail before search, rather than producing incomplete checkpoints.

## Extension And Verification

Register problems, sessions, typed operators, selectors, features, rewards, acceptance, exchange and
budget allocators without editing the scheduler. Generic objective/solution values use registered
lossless codecs; only the adaptation boundary and existing algorithm mathematics perform numeric
conversion. Public learning features/rewards use dimensionless `Flt64`.

Executable coverage lives in `src/test/.../hyper_heuristic/HyperHeuristicTest.kt` and
`SelectionPolicyTest.kt`: configuration equivalence/migration, budgets, all native IDs, composite
and native recovery, asynchronous draining, failures, masked Q/UCB arithmetic, frozen inference,
training splits, multiobjective scalarization and file corruption.

Scope: single JVM, coroutine workers, selection hyper-heuristics and fixed compositions. No algorithm
program generation, database store, distributed scheduler or deep-learning platform is included.


## Adaptive Credit and Orthogonal Portfolios

`OrthogonalOperatorPortfolio.continuous()` returns five niches: distribution estimation, adaptive DE,
encircling, heavy-tail/opposition escape, and deterministic intensification. `permutation()` keeps order
crossover and swap/inversion instead of mixing incompatible encodings. These are small starting portfolios,
not universal performance guarantees: compare against fixed algorithms, random and round-robin at equal budgets.

Both selection levels support `frr-mab` (`window=50`, `exploration=0.5`, `decay=0.8`),
`choice-function` (`rate=0.2`, `recencyWeight=0.1`) and `adaptive-pursuit`
(`rate=0.2`, `minimumProbability=0.01`). Public classes are `FRRMABSelector`,
`ChoiceFunctionSelector`, `AdaptivePursuitSelector`, and the fully configurable `NonStationarySelector`.
FRR ranks positive recent accumulated rewards and adds window-based UCB exploration. Choice-function
combines EWMA individual/pair credit and recency; predecessors are frozen at dispatch, not reconstructed
from asynchronous completion order. Pursuit updates only executed-action quality and moves probabilities
toward the current winner with a minimum exploration floor. Masks renormalize probabilities.
Windows, pair scores, probabilities, dispatch context and revisions restore by action ID; frozen inference
never updates learned parameters.

### Rewards and Numeric Boundaries

New `learning.reward` IDs:

- `dominance`: effective novel non-dominated contribution, not offspring or front count.
- `time-normalized`: `max(0, old-new)/(seconds+epsilonSeconds)`, default `epsilonSeconds=1e-9`.
- `extreme-value`: maximum scalar breakthrough by the actual action in the last `window=50` observations;
  a global sliding window expires old breakthroughs.
- `hypervolume-delta`: multidimensional minimization HV union delta. `reference` is an array of
  normalized worse coordinates (the two-coordinate `referenceX`/`referenceY` form remains accepted).
  `mode="exact"` uses recursive slicing; geometric `maxWork=1000000` exhaustion returns an explicit
  error, never a sampling fallback. High dimensions can explicitly use `mode="sampled"`,
  `samples=16384`, `seed=1`: common random numbers make repeated measurements reproducible.
  `HypervolumeDeltaReward.measure`/`lastMeasurement` expose sample count, standard error and a 99%
  Hoeffding absolute error bound. Geometric work does not invoke the business objective or consume
  objective-evaluation tokens. Sampling uncertainty is not a quality-improvement guarantee.

Omit `reference` to calibrate from `calibrationObservations=32` valid candidate observations, then
freeze a worse point with a 10% margin. Calibration and the freezing action receive zero credit;
subsequent measurements never move the reference. Calibration, frozen coordinates and measurement
diagnostics are checkpointed. Reference overflow is an error requiring an explicit reference.

Scalar gain and HV require `SearchProblem.objectiveCoordinates`: project business values at this adapter
boundary into finite normalized minimization coordinates. Explicitly negate maximization and normalize
units; include the definition in the problem fingerprint. `multiObjectiveProblem` transforms the explicitly
provided directions by default and accepts an override; `singleObjectiveProblem` never guesses numeric
semantics. Scalar-gain rewards reject multiobjective problems. `SelectionTransition.elapsedNanos` records
actual action time. Time-based rewards intentionally remove bitwise reproducibility even in synchronous,
evaluation-budget execution. Workers and coordinator own independent reward instances; stateful reward
windows are checkpointed with their owners. Only used operator actions are credited; unused dispatch
contexts are discarded.

## Heterogeneous Bridging and Intensification

`ContextStateSynchronizer` bridges already re-evaluated imports without collapsing native state:

- PSO matches a nearest destination particle, initializes clamped velocity from import-to-current displacement,
  and retains genuine personal bests. This is not fabricated source-generation history; use
  `velocity(previous,current)` when actual paired source positions are available.
- SHADE/L-SHADE merges populations, archives displaced members within capacity and preserves genuine
  `M_F/M_CR`; positions cannot manufacture successful parameter observations.
- CMA-ES still requires `restartOnImport=true`. Population imports estimate full regularized covariance;
  a single seed recenters while retaining covariance. Evolution paths reset; best history survives.
- Trajectory sessions select a good imported seed. Merely switching scheduling pauses, rather than clears,
  destination state.

New update IDs: `pattern-search` (Hooke-Jeeves; `initialStep=0.1`, `contraction=0.5`, `sweeps=32`),
`opposition`, `pipeline`, `vnd`, `elite-local`, `mutation-stage`, `neighborhood-stage`.
Opposition only reflects coordinates with finite bounds, retaining other coordinates.
Composite `parameters.stages` contains `ComponentSpec` objects; VND accepts `maxTrials`, elite wrapping
accepts `amount`. Mutation/neighborhood stages use `parameters.operator`; mutation additionally accepts
`rate`. Intermediate pipeline solutions are metered and re-evaluated before proceeding. VND restarts at
the first neighborhood on improvement. `EliteLocalSearchWrapper` intensifies Top-K ranked parents;
`NeighborhoodUpdate` lifts trajectory neighborhoods into standard population actions.
Configured composites require explicitly stateless, encoding-compatible children; register a separately
recoverable business composite for stateful children instead of dropping state. All probes, intermediate
and final evaluations consume shared parent tokens. Size slice allowances for dimensionality; exhausted
slices never commit partial native state, although completed valid probes remain globally archivable.

## Acceptance, Permutations and Constraints

`memory.acceptance=ComponentSpec("auto")` selects `AutoAcceptance`: no user-provided temperature,
history length or tolerance is needed. It derives bounded history capacity from the configured global
budget and adjusts delay using recent strict improvements. It compares original business objectives,
supports scalar ordering or Pareto dominance, and is invariant to monotone objective rescaling.
Rejected attempts advance history too. Checkpoints preserve adaptation counters and lossless history.
This is parameter-free at the configuration boundary, not a claim that the implementation has no constants.
The existing default remains `not-worse`.

Explicit alternatives are `late` (`LateAcceptance`, `length=50`) and `record-to-record`
(`RecordToRecordAcceptance`, `initialRatio=0.05`, `horizon=1000`). Both are scalar and checkpoint history.
LAHC is temperature-free and scale-insensitive, but still has a history-length parameter. Record-to-record
scales a decreasing tolerance by observed coordinate magnitude; it is not genuinely parameter-free.
Acceptance only changes the working set, never rolls back private algorithm memory or valid archives.

ALNS building blocks in `heuristic.operator.neighborhood`: `RandomRuin`, `WorstCostRuin`,
`RelatednessRuin`, `GreedyInsertRepair`, `RegretKInsertRepair`, `RuinRecreateOperator`. Items need unique
identity. Typed domain callbacks supply removal/relatedness/insertion estimates, never serialized closures.
Regret insertion prioritizes items with fewer than K feasible positions, rejects non-finite costs and returns
an error on irreparability rather than losing items. Partial insertion estimates must not bypass the budget
with full-objective calls. Evaluate complete neighbors through `SearchProblem.evaluate` in a custom
`SearchSession`; register destroy/repair pairs as distinct actions to reuse selection and coordination.

Native constraint handling needs no custom session. Supply a finite nonnegative normalized
`SearchProblem.constraintViolation` callback (also accepted by the single-/multiobjective convenience
factories), then set each leaf's `AlgorithmConfig.constraints`:

```kotlin
constraints = ConstraintHandlingConfig(
    mode = ConstraintMode.Epsilon,
    horizon = 1000
)
```

Modes are `FeasibilityFirst`, `Epsilon` and `StochasticRanking`. Epsilon's initial scale is calibrated
from initialization unless `initialEpsilon` is explicit, and tightens to zero over complete steps.
GA/swarm/PSO/CLPSO/SHADE/CMA-ES, trajectory acceptance, personal/history bests, imports and NSGA-II/III
use the same typed bridge. NSGA keeps native Pareto/crowding/reference selection within constraint tiers.
Stochastic ranking uses bounded batch passes with its own restorable random stream; pairwise acceptance
and history comparisons remain deterministic feasibility-first comparisons, not randomized comparators.
Partial-budget or failed steps roll back constraint adaptation along with native state.

Explicitly enabling handling relaxes the default feasibility admission, but preserves any custom
`admissible` domain predicate and encoding checks. Raw business objectives are never replaced by
penalties. Strict `feasible` still controls global archives and working solutions; zero violation does
not turn unknown feasibility into proof. Domain violation estimates must not call the full objective
outside the budget, and their normalization belongs in the problem fingerprint. Composite roots reject
constraint configuration; configure leaves. Custom algorithm registrations must declare
`supportsConstraints=true` and honor `compareIndividuals`/`rankIndividuals`; unsupported combinations fail
before search. `SearchProblem.withConstraints` also supports directly opened native runner sessions.
The lower-level `EpsilonConstraintEvaluator.rank` and `StochasticRanking.rank` remain available for custom
domain sessions.

`RuinRecreateSearchSession` can be registered directly without changing the coordinator. Given a domain insertion estimate `cost`:

```kotlin
registry.registerAlgorithm("domain-ruin") { context ->
    Ok(RuinRecreateSearchSession(
        problem = context.problem,
        operator = RuinRecreateOperator(RandomRuin(), RegretKInsertRepair(cost)),
        amount = 2,
        random = context.random,
        statelessEstimates = true
    ))
}
```

Select it with `AlgorithmConfig(id = "ruin", algorithm = ComponentSpec("domain-ruin"))`.
Declare `statelessEstimates=true` only for genuinely stateless domain estimates; otherwise provide `estimateState`.
Complete objectives are metered, while partial insertion estimates must not bypass the budget to call complete objectives.
Coordinator integration tests cover this extension and checkpoint recovery.

## Shared Reward-State Protocol

Controlled sessions share `rewardStateVersion: 1` and `rewardState` with Rust. Global reward state remains
in `components.reward`. Each worker and composite leaf obtains an independent factory instance;
recoverable singleton rewards shared between scopes are rejected before searching. Only invoked slots
advance local reward state: update, mutation, neighborhood, then parameter slots in lexicographic order.
Initialization and unused slots do not receive synthetic reward observations.

Readers accept unversioned legacy `rewardState` and Rust `reward`, but reject conflicting aliases,
unknown versions, and missing versioned state. Custom component payloads still need a shared schema
and identical component/configuration identities. This is not automatic interoperability for arbitrary
closures or the additional adaptive components. Stateless sessions omit these fields and can still be read
by Kotlin `2eb7943`; that original reader cannot restore stateful inner rewards. Rust's
`save_kotlin_checkpoint` targets the original reader; use ordinary `save_checkpoint` for this protocol.
Numerical bit parity and asynchronous scheduling replay are separate contracts, not guaranteed here.
