# Shared BPP Discrete Loading

English | [简体中文](README_ch.md)

## Contract

`DiscreteLoadingProblem<V>` declares cargo IDs, fixed-position IDs, legal assignment
pairs, finite count/weight capacity, fixed occupancy, non-transitive conflict edges,
and optional exactly-one layout groups. `DiscreteLoadingContext<V>` owns sparse binary
assignment, whole-position occupancy and layout decisions. Its aggregation exposes
assignment/count/weight/loaded expressions by stable domain ID, without a dense matrix.

A position may hold several cargo items. Actual assignment, fixed occupancy or any
strictly positive additional weight requires occupancy. `linkAdditionalWeight` uses
actual capacity as the upper bound; it does not invent an epsilon threshold. Empty
reservations are allowed, as exact equivalence with positivity is not a closed MILP set.

Conflicts are covered by verified cliques, including uncovered two-node cliques; no
transitive closure is used. A large position may exclude several small positions
without making the small positions mutually exclusive. Without explicit layout groups,
non-conflicting regions can mix. Shared positions can belong to several layouts.
BPP1D and BPP2D provide separate geometry adapters and do not depend on each other.
Boundary contact is not overlap; different physical-space IDs isolate decks.

## Orchestration and Extensions

`DiscreteLoadingApplicationService<V>` accepts an injected MILP executor, optional
`LinearBendersDecompositionSolver`, and a factory for the full domain registration.
Downstream contexts/pipelines can add variables, objectives and constraints before
returning `RegisteredDiscreteLoading`. The additional-weight extractor participates in
independent final verification. No airline policy or commercial backend belongs here.
Fallback MUST build a new context, variables and full model through the same factory.
The service rejects reuse of the failed context/model.

`LinearBendersAlgorithm` lives in the base framework. Automatic
`LinearBendersPartition` expands function-generated helpers, keeps all integer AND
objective variables in the master, and uses a continuous feasibility subproblem.
This conservative partition is exact but does not promise to outperform direct MILP.
It has no fictitious recourse cost. Custom registrations may supply a minimizing
continuous recourse objective and theta in the master objective.

Extensions can declare `RegisteredDiscreteLoading.bendersStages` using
`LinearBendersStage.Master` or `ContinuousRecourse`. All expanded integer variables
remain in the master; requesting integer recourse is an input error. Continuous
objective terms explicitly placed in recourse are split from the master objective.
Their native bounds must provide a finite, provable lower bound for theta; no arbitrary
large constant is substituted. Objective constants stay in the master and are counted once.

Bindings are variable identities with stable decision IDs; values are mapped by each
model's token table. Optimality cuts use the optimal-point intercept, including
objective constants/direct fixed costs and native-bound contributions. Farkas cuts
project the supporting native bounds and reject invalid/nonseparating certificates
instead of flipping signs. The backend must return original-row dual multipliers;
Farkas multipliers use the Gurobi convention (`<=`: nonnegative, `>=`: nonpositive).
Repeated cuts are deduplicated within one run. Unfixed integer recourse is rejected.

Complete merged solutions are verified against the expanded full model and the
independent discrete contract. The full objective is independently recomputed and checked
against the solver report. Incomplete convergence with an incumbent returns
`Feasible`, not `Optimal`; without an incumbent it returns a structured failure.
Budgets are checked between backend calls; configure backend call time limits too.
`LinearBendersResult.termination` distinguishes convergence, budget, stall and nonoptimal
backend termination. Feasible recourse incumbents are verified without manufacturing an
optimality cut or promoting their status to `Optimal`.

## Partition Configuration Column Generation

`PartitionColumnGenerationApplicationService<V>` is a third, independent strategy usable with
both BPP1D/BPP2D geometry contexts. Columns are **complete partition configurations**, not
single-slot patterns or ordinary bin-packing patterns. This is not combined CG/Benders.

- `LoadingPartition` explicitly and uniquely owns every logical position. No ordering, spatial-name
  or aviation-name inference is used. Columns retain assignments, whole-position occupancy, cargo-use
  vectors, counts/actual weights, compatible layouts, local resource decisions; current row contributions are computed by shared projection.
- The master retains cross-partition cargo uniqueness, geometric conflicts, shared layouts and every
  downstream global row. Empty configurations cannot bypass fixed cargo or fixed occupancy.
- `PartitionLoadingContext` consumes model-owned snapshots and shared column projection **before**
  pricing. Business extensions register through the ordinary context/pipeline path; their objective and
  global-row coefficients automatically enter reduced costs, not only final-model validation.
- `PartitionDecision` explicitly owns estimated/recommended weights or other local decisions; unowned
  variables remain global. `PartitionPricingOracle` is replaceable; the default local MILP pricer retains
  continuous estimates and integer recommendations.
- `PartitionColumnGenerationConfig.batchPricingEnabled` is off by default. When enabled, only the built-in
  `MilpPartitionPricingOracle` batch-prepares non-seed pricing models. Custom oracles and the per-partition
  seed-registration stage keep the scalar path. `PartitionLoadingContext` still adds BPP-local rules, and the
  complete source/master revision is revalidated after every asynchronous solve.
- Registration, adding/removing columns, shadow refresh, restricted LP, final integer master and extraction
  are explicit lifecycle operations. Masters are rebuilt so flattened objectives, rows and tokens stay current.
  A separate artificial-variable feasibility objective avoids arbitrary big-M penalties.
- Final resource completion can optimize downstream resources **inside generated assignment/occupancy
  profiles**, materialize those values as complete new columns, and then solve an exactly-one-column integer
  master. It does not open new profiles or fall back to the original MILP; all original rows and resource
  integrality still apply. `columnLimit` bounds root pricing columns; resource completion may add at most
  one extra column per partition.
- Root LP convergence is not an integer-optimality proof. Without branch-and-price or exhaustive candidate
  certification, successful results are always `Feasible`. Restricted-pool infeasibility is not reported as
  original-problem infeasibility. Resource completion adds no global integer-optimality certificate.

`PartitionMasterRow.sourceName` preserves the downstream row name; explicit IDs survive master
rebuilds, while unbound `model-local-*` IDs are limited to one model session. Column decision values/row coefficients are solver-boundary quantities;
cargo counts and actual cargo weights retain `UInt64` and `Quantity<V>`.

## Verification

Default tests exercise interval sweep vs seeded pairwise enumeration, sparse models,
whole-position occupancy, layout selection and Benders certificate/identity handling.
Logs and reports belong in the OS temporary directory. Demo2's commercial end-to-end
checks are isolated under its `demo2-gurobi-e2e` profile.

## Dynamic Row Integration

`PartitionLoadingContext` now delegates model snapshots/revisions to core `LinearModelState` and column mapping, master assembly,
local linear pricing and dual projection to shared `ColumnProjection`. It retains only loading-specific ownership, geometry,
configuration validation and result adaptation. Columns no longer store stale row-contribution snapshots.
Register late business rows with `source.state.registerRow(ConstraintId("business:limit"), relation)`; existing/new columns,
LP pricing and final/refinement masters use the same source row without application changes.
Pricing/master/column-insertion/extraction boundaries use suspend Ret/Try validation. Seeding explicitly uses `seed = true`;
dual refresh requires the registration that produced an optimal LP, and missing rows are not silently zero.
The application prices every block against one dual snapshot before batch insertion.
Explicit row IDs survive rebuilds; `model-local-*` IDs are limited to one model session. This is capability plumbing, not joint CG/Benders.
