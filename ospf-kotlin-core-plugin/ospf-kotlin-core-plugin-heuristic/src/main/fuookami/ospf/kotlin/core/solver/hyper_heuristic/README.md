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
