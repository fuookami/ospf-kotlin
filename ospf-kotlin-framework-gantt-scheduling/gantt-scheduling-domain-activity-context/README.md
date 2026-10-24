# Finite-Domain Activity Scheduling

English | [简体中文](README_ch.md)

This module registers activity-level MILP models independently of bunch generation. It reuses Gantt `TaskId`, `ExecutorId`, `TimeRange`, and `SchedulingSolverValueAdapter`, without depending on Network or forcing projects and joint-resource operations into paths.

## Business Contract

- `ActivitySchedulingProblem<V>` is an input snapshot; create a fresh `ActivitySchedulingContext` for each model.
- An `Activity` has alternative `ActivityMode`s declaring complete allowed start sets, duration, cost, and resource phases. These finite sets define the business domain, not a search budget.
- Exactly one execution choice is selected per activity. Cancellation requires an explicit `cancellationCost`. A mandatory activity without candidates produces an infeasible model, not a construction error.
- `ActivityResourceUse` specifies a resource reservation relative to activity start. Equipment, personnel, and tools may be reserved simultaneously and for different phases. Overlapping uses of the same resource are additive.
- `ActivityResource` supplies named capacity, unavailable intervals, and non-overlapping capacity overrides. Existing `WorkingCalendar.unavailableTimes` can be passed directly. Unit-capacity named workers express individual exclusivity; skill-group headcounts alone do not.
- Capacity rows cover every reservation/calendar/capacity event segment using half-open intervals. Mid-activity capacity drops are checked, with no implicit preemption across breaks.
- Zero-duration modes represent milestones. Automatic preemption, splitting, recovery, sequence-dependent setup, and full workforce regulations are not implemented here.

## Temporal Materials and Inventory

Capacity assembly groups reservations once by resource and sweeps their endpoints;
only choices whose active reservations change are recomputed. Inventory assembly
groups material events once and advances per-choice prefixes. Both retain the original
input-occurrence floating-point accumulation order: removals or earlier insertions
recompute the affected group rather than subtracting or reassociating values.
Output work still depends on the generated nonzero coefficients; adversarial unordered
or heavily overlapping groups can still require repeated local folds.

`ActivityMaterial<V>` declares named opening stock, a minimum (zero by default), and an optional storage limit. `ActivityMode.materialFlows` contains signed stock changes at offsets within mode duration: positive quantities supply, negative quantities consume. These cover start consumption, completion production, and intermediate events. Materials never compensate one another, and quantities normalize to the opening-stock unit.

`problem.materialEvents` supplies fixed future receipts or demands. Rows check opening stock before horizon-start events and cumulative stock after every event, including both horizon endpoints. Future production cannot cover past shortages. Simultaneous events settle atomically as a net change; if unloading order or transient storage peaks matter, declare distinct event times or explicit intermediate activities.

For rolling recovery, opening stock already reflects historical execution. `Running`/`Completed` facts do not automatically regenerate material events. Explicit future fixed events describe remaining completion production, avoiding duplicate consumption or receipt.

Stock bounds are hard by default. An explicit demand-activity `cancellationCost` permits bounded whole-order non-service, retaining the cancelled task and penalty in the result; this is not arbitrary-quantity shortage, continuous borrowing, partial fulfillment, automatic substitution, or full BOM/MRP. Those capabilities remain outside this increment.

`ActivitySchedule.materialInventory` retains opening and post-event stock for each material. Independent verification replays selected events without reading inventory rows. Impossible fixed receipts/demands remain normal model infeasibility, even when no pending activities exist.

## Dependencies

`ActivityDependency.from` and `.to` select Start/Finish endpoints, supporting FS, SS, FF, and SF. The endpoint difference obeys a minimum and optional maximum lag, including negative lags.

Presence is explicit: `WhenBothPresent` only checks jointly present activities; the default `RequirePredecessor` requires a predecessor for a selected successor; `RequireBoth` couples both presences. Valid synchronization cycles are allowed, while contradictory timing yields infeasibility. Incompatible finite choices use binary exclusion rows, without arbitrary big-M constants.

## Baselines and Execution Facts

`ActivityBaseline` freezes a choice or charges one-off start/mode change costs. A non-frozen historical choice may lie outside the new domain; a frozen choice must still exist. Cancellation incurs its explicit cost without extra start/mode change charges.

Execution facts and pending activities have disjoint task identities. `Completed` facts finish no later than the horizon start, retain actual timing for dependencies, and do not add sunk costs. `Running` facts start no later than the horizon start and explicitly provide expected completion plus remaining reservations starting at or after the optimization origin. No full-work restart or automatic remaining-duration inference occurs. Historical capacity outside the optimization horizon is not audited.

## Modeling and Solving

Create a context with `ActivitySchedulingContext.create(problem, adapter)`, handle `Ok`/`Failed`/`Fatal`, then register with an existing model. Embedded registration never invokes a solver. `register(model, minimizeCost = false)` permits caller-owned objectives; the domain cost remains available as `aggregation.compilation.cost`.

The application module provides `ActivitySchedulingApplicationService<V>`, which creates fresh state, uses the same registration path, calls the injected solver, preserves structured terminal outcomes, and extracts a validated schedule. It uses the MILP capability of the existing `ColumnGenerationSolver` abstraction without running column generation.

## Extensions and Numeric Boundaries

`ActivityRow` supplies unit-aware choice/cancellation coefficients and bounds for coverage, budgets, and quotas. `additionalCost` computes complete-choice contributions such as due-date and preference costs once during context creation. `aggregation.variable(key)`, `cancellationVariable(task)`, and the application `extra(context, model)` hook support downstream model extensions.

Resource quantities and costs retain `Quantity<V>`; time uses `Instant`/`Duration`. Reservations normalize to their resource capacity unit and costs to `costUnit` at the solver boundary. Different resources are never dimensionally mixed. Affine units, incompatible dimensions, non-finite conversion, nonzero-to-zero underflow, and conversion failures return errors. Solving uses `Flt64`, not arbitrary-precision optimization; extreme magnitudes still require business scaling.

## Validation and Proof Scope

`validate(choiceKeys)` independently checks selection, freezing, endpoint dependencies, event-based capacity, inventory, and declared extension rows without reading generated model constraints. Missing optional activities mean explicit cancellation. Unknown or duplicate choices produce infeasibility diagnostics.

`extractSolution` additionally checks model ownership, binary values, exactly one selection per activity, and all current model constraints, including caller-added rows. `ActivitySchedule` retains complete mode/timing, internal reservations, execution facts, cancellations, cost, and candidate scope.

Solver optimality applies **only to the declared finite candidate model**, not to unrestricted continuous time or global integer optimality of existing bunch generation. No-incumbent limits, cancellation, and infeasibility remain structured terminal outcomes.

## Verification and Remaining Scope

Default tests require no native solver and include a manual flexible-job-shop oracle, a resource-constrained project, phased equipment/personnel scheduling, rolling recovery, all endpoint relation types, time-varying capacity, extra coverage constraints, exhaustive binary temporal-inventory cases, simultaneous settlement, explicit order cancellation, and `Flt64`/`FltX` unit conversion and underflow rejection. Application profile `gantt-scip-integration-tests` checks actual solves; unavailable native runtimes do not count as verified.

This increment covers shared foundations and hard temporal inventory, not incomplete legacy task/resource/produce registration paths or a complete APS/MPS/LSP product. Sequence state, maintenance, continuous soft shortages, workforce, energy, and Core CP compilation remain subsequent work.
