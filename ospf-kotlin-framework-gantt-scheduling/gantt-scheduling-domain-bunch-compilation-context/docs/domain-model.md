# Bunch Compilation Bounded Context — Domain Model

[toc]

## 1. Overview

The Bunch Compilation context implements the orchestration layer for batch-level column generation in gantt scheduling. Groups of tasks ("bunches") are iteratively generated and compiled into an LP model via column generation. Each bunch represents a feasible assignment of multiple tasks to one executor. The context manages the full lifecycle of bunch columns: registration, addition, removal, fixing, shadow price extraction, and solution analysis.

### 1. Dependent Contexts

1. **Gantt Scheduling Infrastructure** (gantt-scheduling-infrastructure) — TimeWindow, TimeSlot, TimeRange
2. **Gantt Scheduling Task Domain** (gantt-scheduling-domain-task-context) — AbstractTask, AbstractTaskBunch, Executor, AssignmentPolicy
3. **Gantt Scheduling Task Compilation** (gantt-scheduling-domain-task-compilation-context) — Compilation, TaskSolution, TaskTimeImpl, BunchSchedulingTaskTime
4. **Gantt Scheduling Capacity Scheduling** (gantt-scheduling-domain-capacity-scheduling-context) — ProductionAction, ActionAllocation, CapacityCompilation, CapacityColumn, Capacity
5. **OSP Framework Core** (ospf-kotlin-core) — MetaModel, BinVariable, LinearIntermediateSymbols, SlackFunction
6. **OSP Framework** (ospf-kotlin-framework) — ShadowPrice, ShadowPriceKey

---

## 2. Concepts / Entities

### 1. Bunch (B)

A group of tasks assigned to a single executor; a column in the column generation formulation. Each bunch represents a feasible scheduling plan for one executor across multiple tasks.

**$tasks_{b}$** : The set of tasks contained in bunch $b$.
**$executor_{b}$** : The executor assigned to bunch $b$.
**$cost_{b}$** : The cost of bunch $b$, used in the objective function.
**$time_{b}$** : The time information of bunch $b$, including start and end times for each task.

### 2. SlotBasedBunch

Extends Bunch with time slot awareness, binding each bunch to exactly one discrete time interval.

**$slot_{b}$** : The time slot to which bunch $b$ belongs.
**$slotIndex_{b}$** : The index of the time slot within the slot sequence.

### 3. BunchAggregation\<B, V, T, E, A\>

Container managing the lifecycle of bunches across column generation iterations. Handles registration, deduplication, removal, and iteration tracking.

**$bunchesIteration_{a}$** : The mapping from iteration number to the set of bunches added in that iteration.
**$bunches_{a}$** : The accumulated set of all bunches across all iterations.
**$removedBunches_{a}$** : The set of bunches that have been removed during column management.
**$lastIterationBunches_{a}$** : The set of bunches added in the most recent iteration.

### 4. BunchCompilation\<B, V, T, E, A\>

The LP model structure for bunch-based scheduling via column generation. Contains decision variables, constraints, and intermediate symbols for the master problem.

**$tasks_{c}$** : The set of all tasks in the compilation.
**$executors_{c}$** : The set of all executors in the compilation.
**$lockCancelTasks_{c}$** : The set of tasks forced to be canceled (locked to $y[t] = 1$).
**$withExecutorLeisure_{c}$** : Whether executor leisure variables ($z[e]$) are enabled.
**$taskCancelEnabled_{c}$** : Whether task cancellation variables ($y[t]$) are enabled.
**$aggregation_{c}$** : The bunch aggregation container managing bunch lifecycle.
**$x_{c}$** : Per-iteration binary decision variables for bunch selection.
**$y_{c}$** : Binary decision variables for task cancellation.
**$z_{c}$** : Binary decision variables for executor leisure.
**$bunchCost_{c}$** : Intermediate symbol representing total bunch cost.
**$taskAssignment_{c}$** : Intermediate symbol matrix for task-executor assignment.
**$taskCompilation_{c}$** : Intermediate symbol for task coverage (bunches + cancellation).
**$executorCompilation_{c}$** : Intermediate symbol for executor coverage (bunches + leisure).

### 5. SlotBasedBunchAggregation

Extends BunchAggregation with slot-aware deduplication, ensuring bunches are compared within their time slot context.

### 6. SlotBasedBunchCompilation

Extends BunchCompilation with slot-based structure, organizing bunches and variables by time slot.

**$slots_{c}$** : The set of time slots in the compilation.
**$bunchesBySlot_{c}$** : Mapping from time slot to the set of bunches in that slot.
**$variableByBunch_{c}$** : Authoritative one-to-one mapping from each active bunch to its actual binary column variable.
**$xBySlot_{c}$** : Mapping from time slot to actual binary column variables, in exactly the same order as `bunchesBySlot`.
**$executorSlotCompilation_{c}$** : Linear expression for each `(executor, slot)` pair. `ExecutorSlotCompilationConstraint` can enforce that exactly one explicit working or idle column is selected for every pair and exposes a typed shadow-price key.

Column removal is reflected by the active mapping views: removed bunches no longer appear in `bunchesBySlot`, `variableByBunch`, or `xBySlot`.

### 7. BunchSolution\<B, V, T, E, A\>

The solution entity extracted after solving the LP model, containing selected bunches, canceled tasks, and summary statistics.

**$bunches_{s}$** : The set of bunches selected in the solution (with $x[b] > 0$).
**$canceledTasks_{s}$** : The set of tasks canceled in the solution (with $y[t] > 0$).
**$summary_{s}$** : The solution summary containing aggregate statistics.

### 8. BunchSolutionSummary

Aggregate statistics of a bunch scheduling solution.

**$bunchCount_{s}$** : Number of selected bunches in the solution.
**$assignedTaskCount_{s}$** : Number of tasks assigned to executors.
**$canceledTaskCount_{s}$** : Number of tasks canceled.
**$totalTaskCount_{s}$** : Total number of tasks (assigned + canceled).

### 9. SlotBasedCapacityResult\<A, M, R, V\>

Capacity pre-solve results for a single time slot, providing production/consumption/resource bounds.

**$slot_{r}$** : The time slot for which this result applies.
**$slotIndex_{r}$** : The index of the time slot.
**$actionAllocations_{r}$** : The set of action allocations determined by the capacity pre-solve.
**$totalCostQuantityValue_{r}$** : The total cost quantity value for the slot.
**$produceQuantityByProduct_{r}$** : Mapping from product to production quantity in this slot.
**$consumptionQuantityByMaterial_{r}$** : Mapping from material to consumption quantity in this slot.
**$resourceUsageQuantityByResource_{r}$** : Mapping from resource to usage quantity in this slot.

### 10. SlotConstraints\<M, R, V\>

Upper and lower bounds on production, consumption, and resource usage per time slot, derived from capacity pre-solve.

**$maxProduceQuantity_{sc}(m)$** : Maximum production quantity for product $m$ in this slot.
**$minProduceQuantity_{sc}(m)$** : Minimum production quantity for product $m$ in this slot.
**$maxConsumptionQuantity_{sc}(m)$** : Maximum consumption quantity for material $m$ in this slot.
**$minConsumptionQuantity_{sc}(m)$** : Minimum consumption quantity for material $m$ in this slot.
**$maxResourceUsageQuantity_{sc}(r)$** : Maximum resource usage quantity for resource $r$ in this slot.
**$minResourceUsageQuantity_{sc}(r)$** : Minimum resource usage quantity for resource $r$ in this slot.

### 11. TaskReverse\<T, E, A\>

Pairs of tasks whose temporal order can be swapped as a neighborhood search move, used to explore alternative scheduling sequences.

**$symmetricalPairs_{tr}$** : The set of reversible task pairs where both tasks share the same executor.
**$leftMapper_{tr}$** : Mapping from reversible pair to the left (preceding) task.
**$rightMapper_{tr}$** : Mapping from reversible pair to the right (succeeding) task.

### 12. TaskReverse.ReversiblePair

A single pair of tasks whose temporal order may be reversed.

**$prevTask_{rp}$** : The task that currently precedes the other in the schedule.
**$succTask_{rp}$** : The task that currently succeeds the other in the schedule.
**$symmetrical_{rp}$** : Whether both tasks in the pair share the same executor.

### 13. BunchSchedulingTaskTime

Extends TaskTimeImpl with column generation-specific time estimation capabilities.

**$compilation_{tt}$** : The bunch compilation this task time belongs to.
**$redundancyRange_{tt}$** : The allowed deviation range for start-time estimation.
**$estRedundancy_{tt}$** : The redundancy variable for start-time estimation, domain is $[-redundancy, +redundancy]$.
**$estSlack_{tt}$** : The slack between estimated start time and scheduled start time.
**$estimateStartTime_{tt}$** : The estimated start time of the task, computed from redundancy and weighted bunch start times.
**$estimateEndTime_{tt}$** : The estimated end time of the task, computed from redundancy and weighted bunch end times.

---

## 3. Variables

### 1. Decision Variables

**$x_{i}[b]$** : Binary selection variable for bunch $b$ in iteration $i$, dimensionless, domain is $\{0, 1\}$, representing whether bunch $b$ is selected in the solution at iteration $i$, $\forall b \in B_{i}$.

**$y[t]$** : Binary cancellation variable for task $t$, dimensionless, domain is $\{0, 1\}$, representing whether task $t$ is canceled (not assigned to any executor), $\forall t \in T$. Locked to $1$ for $t \in T^{lockCancel}$.

**$z[e]$** : Binary leisure variable for executor $e$, dimensionless, domain is $\{0, 1\}$, representing whether executor $e$ is idle (not assigned any bunch), $\forall e \in E$. Only present when $withExecutorLeisure = true$.

### 2. Auxiliary Variables

**$estRedundancy[t]$** : Start-time redundancy variable for task $t$, real-valued, domain is $[-redundancy, +redundancy]$, representing the allowed deviation from the weighted average start time, $\forall t \in T$.

---

## 4. Predicates

### 1. Slot-Based Predicates

**SlotBasedBunch(b)** : Bunch $b$ belongs to a time slot, i.e., $b$ has an associated $slot_{b}$ and $slotIndex_{b}$.

### 2. Deduplication Predicates

**sameColumnAs(b1, b2)** : Bunches $b_1$ and $b_2$ are duplicates, i.e., they have identical task sets, executor, and (if slot-based) time slot.

### 3. Task Reverse Predicates

**reverseEnabled(t1, t2)** : Tasks $t_1$ and $t_2$ can have their temporal order swapped, i.e., the pair is a valid neighborhood move.

**symmetrical(rp)** : Reversible pair $rp$ is symmetrical, i.e., both tasks $prevTask_{rp}$ and $succTask_{rp}$ share the same executor.

---

## 5. Sets

### 1. Tasks

**$T$** : Universal set of all tasks.

**$T^{lockCancel}$** : Tasks forced to be canceled, i.e., locked to $y[t] = 1$.

### 2. Executors

**$E$** : Universal set of all executors.

**$E^{hidden}$** : Subset of executors with $z[e] > 0$, i.e., executors that are idle in the current solution.

### 3. Bunches

**$B$** : Universal set of all accumulated bunches across all iterations.

**$B_{i}$** : Bunches added in iteration $i$, i.e., $B_{i} = bunchesIteration(i)$.
**$B^{removed}$** : Bunches removed during column management, i.e., $B^{removed} = removedBunches$.
**$B^{fixed}$** : Bunches fixed to $x[b] = 1$, i.e., bunches locked into the solution.
**$B^{kept}$** : Bunches with positive LP value ($x[b] > 0$) in the current solution.

### 4. Time Slots

**$S$** : Universal set of all time slots.

**$B_{s}$** : Bunches belonging to time slot $s$, i.e., $B_{s} = \{b \in B \mid slot_{b} = s\}$.

---

## 6. Intermediate Values

### 1. Bunch Cost

**Description**: The total cost of all selected bunches, formed as the weighted sum of individual bunch costs by their selection variables.

$$
bunchCost = \sum_{b \in B} cost(b) \cdot x[b]
$$

### 2. Task Assignment

**Description**: For each task-executor pair, the sum of selection variables for all bunches containing that task assigned to that executor.

$$
taskAssignment[t, e] = \sum_{\substack{b \in B \\ t \in tasks(b) \\ executor(b) = e}} x[b], \; \forall t \in T, \forall e \in E
$$

### 3. Task Compilation

**Description**: For each task, the cancellation variable plus the sum of selection variables for all bunches containing that task. This must equal 1 for proper coverage.

$$
taskCompilation[t] = y[t] + \sum_{\substack{b \in B \\ t \in tasks(b)}} x[b], \; \forall t \in T
$$

### 4. Executor Compilation

**Description**: For each executor, the leisure variable plus the sum of selection variables for all bunches assigned to that executor. This must equal 1 for proper coverage.

$$
executorCompilation[e] = z[e] + \sum_{\substack{b \in B \\ executor(b) = e}} x[b], \; \forall e \in E
$$

### 5. Estimate Start Time

**Description**: The estimated start time of a task, computed as the redundancy offset plus the weighted sum of start times from all bunches containing the task.

$$
estimateStartTime[t] = estRedundancy[t] + \sum_{\substack{b \in B \\ t \in tasks(b)}} time.start(b, t) \cdot x[b], \; \forall t \in T
$$

### 6. Estimate End Time

**Description**: The estimated end time of a task, computed as the redundancy offset plus the weighted sum of end times from all bunches containing the task.

$$
estimateEndTime[t] = estRedundancy[t] + \sum_{\substack{b \in B \\ t \in tasks(b)}} time.end(b, t) \cdot x[b], \; \forall t \in T
$$

### 7. Estimate Slack

**Description**: The slack between the estimated start time and the scheduled start time, computed via the SlackFunction. Its definition is further specified in *SlackFunction*.

$$
estSlack[t] = Slack(estimateStartTime[t], scheduled\_start[t]), \; \forall t \in T
$$

### 8. Next Reduced Cost Cutoff

**Description**: The reduced cost cutoff for progressive column removal, computed as the maximum of two-thirds of the maximum reduced cost and a minimum threshold of 5.0.

$$
nextReducedCostCutoff(max) = \max(\lfloor maxReducedCost \rfloor \times \frac{2}{3}, \; 5.0)
$$

---

## 7. Assertions

### 1. Removed Bunch Exclusion

**Description**: When fixing a bunch, it must not be in the removed bunches set. A removed bunch cannot be fixed.

$$
\forall b \in B \; (fix(b) \rightarrow b \notin B^{removed})
$$

### 2. Compilation Initialization Completeness

**Description**: During BunchCompilation initialization, all executors and tasks must be indexed. The sets $T$ and $E$ must be fully enumerated before LP construction.

$$
|tasks_{c}| = |T| \wedge |executors_{c}| = |E|
$$

### 3. Task Reverse Validity

**Description**: For each task pair considered for reversal, the reverseEnabled predicate must hold. Only valid pairs can be added to the TaskReverse structure.

$$
\forall (t_1, t_2) \in TaskReverse \; (reverseEnabled(t_1, t_2))
$$

### 4. Non-Empty Column Addition

**Description**: When adding columns via BunchSchedulingTaskTime, the bunches set must be non-empty. Adding zero columns is an error.

$$
|bunches| > 0
$$

---

## 8. Constraints

### 1. Task Cancellation Variable Range

**[CN]**: 任务取消变量范围约束
**Description**: For tasks in the lock-cancel set, the cancellation variable is locked to 1. For all other tasks, the cancellation variable is binary $[0, 1]$.

$$
s.t. \quad y[t] = 1, \; \forall t \in T^{lockCancel}
$$

$$
s.t. \quad y[t] \in \{0, 1\}, \; \forall t \in T \setminus T^{lockCancel}
$$

### 2. Bunch Selection Variable Range

**[CN]**: 批次选择变量范围约束
**Description**: Each bunch selection variable is binary, indicating whether the bunch is selected in the solution.

$$
s.t. \quad x[b] \in \{0, 1\}, \; \forall b \in B
$$

### 3. Executor Leisure Variable Range

**[CN]**: 执行器空闲变量范围约束
**Description**: Each executor leisure variable is binary, indicating whether the executor is idle. Only present when executor leisure is enabled.

$$
s.t. \quad z[e] \in \{0, 1\}, \; \forall e \in E \mid withExecutorLeisure
$$

### 4. Slot Production Bounds

**[CN]**: 时间槽产量上下界约束
**Description**: The production quantity for each product in each time slot must lie within the bounds determined by the capacity pre-solve.

$$
s.t. \quad minProduceQuantity_{s}(m) \leq produce_{s}(m) \leq maxProduceQuantity_{s}(m), \; \forall s \in S, \forall m \in M
$$

### 5. Slot Consumption Bounds

**[CN]**: 时间槽消耗量上下界约束
**Description**: The consumption quantity for each material in each time slot must lie within the bounds determined by the capacity pre-solve.

$$
s.t. \quad minConsumptionQuantity_{s}(m) \leq consumption_{s}(m) \leq maxConsumptionQuantity_{s}(m), \; \forall s \in S, \forall m \in M
$$

### 6. Slot Resource Usage Bounds

**[CN]**: 时间槽资源使用量上下界约束
**Description**: The resource usage quantity for each resource in each time slot must lie within the bounds determined by the capacity pre-solve.

$$
s.t. \quad minResourceUsageQuantity_{s}(r) \leq resourceUsage_{s}(r) \leq maxResourceUsageQuantity_{s}(r), \; \forall s \in S, \forall r \in R
$$

---

## 9. Objective Function

### 1. Bunch Cost Minimization

**Description**: Minimize the total cost of all selected bunches, guiding the solver toward lower-cost task-to-executor assignments.

$$
\min \; bunchCost = \sum_{b \in B} cost(b) \cdot x[b]
$$

---

## 10. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Column Generation | Built-in framework | Sections 3, 6, 9 | Iterative bunch generation and RMP solving |
| SlackFunction | Built-in framework | Section 6 (Estimate Slack) | Computes slack between estimated and scheduled start times |
| Capacity Pre-Solve | gantt-scheduling-domain-capacity-scheduling-context | Section 8 (Slot Constraints) | Determines per-slot production/consumption/resource bounds |
| Task Reverse | Built-in | Section 2 (TaskReverse) | Neighborhood search move swapping temporal order of task pairs |

---

## 11. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 任务 | $t$ / T | Task | An atomic schedulable unit |
| 执行器 | $e$ / E | Executor | A resource that executes tasks |
| 分配策略 | $A$ / Assignment Policy | Assignment Policy | Rules for task assignment |
| 批次 | $b$ / Bunch | Bunch | A group of tasks on one executor; a CG column |
| 批次迭代 | $B_i$ / Bunch Iteration | Bunch Iteration | Bunches added per CG iteration |
| 编排 | BunchCompilation | Compilation | LP model structure for bunch scheduling |
| 聚合 | BunchAggregation | Aggregation | Container managing bunch lifecycle |
| 列选择变量 | $x_i[b]$ / Column | Column | Binary: is bunch b selected? |
| 取消变量 | $y[t]$ / Cancel Variable | Cancel Variable | Binary: is task t canceled? |
| 空闲变量 | $z[e]$ / Leisure Variable | Leisure Variable | Binary: is executor e idle? |
| 批次成本 | bunchCost | Bunch Cost | Sum of selected bunch costs |
| 任务编排 | $taskCompilation[t]$ | Task Compilation | Bunches covering task t + cancellation |
| 执行器编排 | $executorCompilation[e]$ | Executor Compilation | Leisure + bunches on executor e |
| 任务分配 | $taskAssignment[t,e]$ | Task Assignment | Bunches with task t on executor e |
| 影子价格 | ShadowPrice | Shadow Price | Dual values for pricing |
| 约简成本 | Reduced Cost | Reduced Cost | Cost minus dual contribution |
| 固定批次 | $B^{fixed}$ / Fixed Bunch | Fixed Bunch | Bunches locked to x=1 |
| 保留批次 | $B^{kept}$ / Kept Bunch | Kept Bunch | Bunches with positive LP value |
| 隐藏执行器 | $E^{hidden}$ / Hidden Executor | Hidden Executor | Executors with z > 0 |
| 时间槽 | $s$ / TimeSlot | Slot | A discrete time interval |
| 容量预求解 | SlotBasedCapacityPreSolver | Capacity Pre-Solve | Gets slot-level intermediate values |
| 时间槽约束 | SlotConstraints | Slot Constraints | Bounds per slot |
| 任务反转 | TaskReverse | Task Reverse | Pairs whose order can be swapped |
| 对称对 | symmetrical / Symmetrical Pair | Symmetrical Pair | Reversible pair on same executor |
| 预估开始时间 | $estimateStartTime[t]$ | Estimate Start Time | Redundancy + weighted start times |
| 预估结束时间 | $estimateEndTime[t]$ | Estimate End Time | Redundancy + weighted end times |
| 最大完工时间 | Makespan | Makespan | Maximum completion time |
| 冗余范围 | $redundancyRange$ | Redundancy Range | Allowed deviation for time estimation |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Column Generation Architecture | Direct MILP / Decomposition | Dantzig-Wolfe decomposition handles large bunch spaces efficiently; master problem solved iteratively with pricing subproblem | 2024 |
| Two-Level Aggregation | Single aggregation | BunchAggregation for storage/dedup, AbstractBunchCompilationAggregation for LP management; separation of concerns | 2024 |
| Slot-Based Extension | Slot-free bunch model | Bunch belongs to exactly one slot; enables per-slot capacity constraints and parallel solving | 2024 |
| Capacity Pre-Solve | Inline capacity constraints | Separate LP before main CG loop determines slot-level bounds; reduces main problem complexity | 2024 |
| Progressive Column Removal | Remove all / Remove none | 2/3 factor with minimum 5 threshold; balances model size and solution quality | 2024 |
| Local Fixing Strategy | No fixing / Global fixing | Fix best bunches locally; fallback to single best; reduces branching space | 2024 |
| Flt64 Solver Boundary | Domain-level solver types | Domain types converted to Flt64 post-solve; decouples business logic from solver numerics | 2024 |
| Task Reverse Modeling | Fixed sequencing | Neighborhood search moves swap temporal order of task pairs; explores alternative schedules | 2024 |
| Data Object Analyzers | Mutable solution objects | Stateless solution reconstruction from data objects; thread-safe and reproducible | 2024 |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial bunch compilation model | Basic column generation framework for gantt scheduling |
| v1.1 | Added slot-based extension | Support for time-slot-aware scheduling with per-slot capacity constraints |
| v1.2 | Added capacity pre-solve integration | Separate LP for slot-level bounds reduces main problem complexity |
| v1.3 | Added task reverse neighborhood moves | Explore alternative scheduling sequences via temporal order swapping |
| v1.4 | Added progressive column removal | Control model size growth across iterations |
| v1.5 | Added executor leisure variables | Model idle executors explicitly for better resource utilization analysis |
