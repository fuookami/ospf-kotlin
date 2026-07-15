# Capacity Scheduling Bounded Context — Domain Model

[toc]

## 1. Overview

Determines how to allocate production actions across executors over discretized time slots, subject to capacity constraints, while minimizing cost. Supports three strategies: direct (2D), ordered (3D with binary linking), and iterative/column generation.

### 1. Dependent Contexts

1. **Gantt Scheduling Infrastructure** (gantt-scheduling-infrastructure) — TimeSlot, TimeWindow, TimeRange
2. **Gantt Scheduling Task Domain** (gantt-scheduling-domain-task-context) — Executor, SchedulingSolverValueAdapter
3. **OSPF Core** (ospf-kotlin-core) — LinearMetaModel, LinearExpressionSymbols, UIntVariable, BinVariable
4. **OSPF Math** (ospf-kotlin-math) — RealNumber, Flt64, UInt64, LinearMonomial, LinearPolynomial
5. **OSPF Quantities** (ospf-kotlin-quantities) — Quantity, PhysicalUnit

---

## 2. Concepts / Entities

### 1. ProductionAction

A way to produce capacity on an executor, either as discrete batches or continuous duration.

**$id_{a}$** : Unique identifier of production action $a$.
**$name_{a}$** : Name of production action $a$.
**$displayName_{a}$** : Display name of production action $a$.
**$executor_{a}$** : Executor that production action $a$ belongs to.
**$discrete_{a}$** : Whether production action $a$ uses discrete batch-count mode (otherwise duration-unit mode), boolean.
**$batchDuration_{a}$** : Fixed duration per batch of production action $a$, physical quantity in time units.
**$unitCapacity(a, tw)$** : Unit capacity of production action $a$ within time window $tw$.
**$unitCost(a, t)$** : Unit cost of production action $a$ at time $t$, converted from double.
**$upperBound(a, s, tw)$** : Upper bound of production action $a$ for time slot $s$ and time window $tw$.
**$unitCapacityQuantity_{a}$** : Unit capacity physical quantity of production action $a$.
**$unitCostQuantity_{a}$** : Unit cost physical quantity of production action $a$.

### 2. CapacityColumn

A complete allocation plan for one executor at one time slot. Serves as the fundamental column unit in column generation.

**$executor_{c}$** : Executor of capacity column $c$.
**$slotIndex_{c}$** : Time slot index of capacity column $c$.
**$order_{c}$** : Order position of capacity column $c$ (used in ordered variant).
**$allocations_{c}$** : Allocation map of capacity column $c$, keyed by production action with UInt64 quantity values.
**$columnCost_{c}$** : Total cost of capacity column $c$.
**$totalAmount_{c}$** : Total allocation quantity of capacity column $c$, derived from the allocation map.
**$isEmpty(c)$** : Whether capacity column $c$ is empty (allocation map is empty or all zeros), boolean predicate.
**$amountFor(c, a)$** : Allocation quantity of production action $a$ in capacity column $c$.
**$costQuantity(c, u)$** : Cost physical quantity of capacity column $c$ in unit $u$.

### 3. CapacitySchedulingSolution

The result entity of scheduling, containing all action allocations and executor capacity information.

**$actions_{sol}$** : Set of all production actions involved in the solution.
**$actionAllocations_{sol}$** : List of action allocations in the solution, each element being an ActionAllocation.
**$executorCapacities_{sol}$** : List of executor capacity usage results in the solution.
**$allocationsBySlot_{sol}$** : Allocation map grouped by time slot.
**$capacitiesBySlot_{sol}$** : Capacity usage map grouped by time slot.

### 4. ActionAllocation

An allocation record of a production action at a time slot.

**$action_{alloc}$** : The production action of the allocation.
**$slot_{alloc}$** : The time slot of the allocation.
**$slotIndex_{alloc}$** : Time slot index of the allocation.
**$amount_{alloc}$** : Quantity of the allocation.
**$duration_{alloc}$** : Duration of the allocation.
**$order_{alloc}$** : Order position of the allocation.
**$durationQuantity(alloc, tw, u)$** : Duration physical quantity of the allocation in time window $tw$ and unit $u$.

### 5. ExecutorCapacityResult

Capacity usage per executor per time slot.

**$executor_{ecr}$** : Executor of the capacity result.
**$slot_{ecr}$** : Time slot of the capacity result.
**$slotIndex_{ecr}$** : Time slot index of the capacity result.
**$totalDuration_{ecr}$** : Total duration of this executor at this time slot.

### 6. CapacitySchedulingAggregation

Aggregation of production actions and time slots, providing a global view.

**$actions_{agg}$** : List of all production actions.
**$slots_{agg}$** : List of all time slots.
**$timeWindow_{agg}$** : Scheduling time window.
**$actionsByExecutor_{agg}$** : Production action map grouped by executor.
**$slotCount_{agg}$** : Number of time slots, derived from $slots_{agg}$.
**$actionCount_{agg}$** : Number of production actions, derived from $actions_{agg}$.
**$executorCount_{agg}$** : Number of executors, derived from $actionsByExecutor_{agg}$.

### 7. CapacityColumnAggregation

Manages capacity columns across iterations, supporting addition, removal, and clearing.

**$columnsIteration_{ca}$** : Column map grouped by iteration.
**$columns_{ca}$** : List of all currently active columns.
**$removedColumns_{ca}$** : List of removed columns.
**$addColumns(cols)$** : Add a set of new columns.
**$removeColumn(col)$** : Remove a specified column.
**$clear()$** : Clear all columns.

### 8. SchedulingSolverValueAdapter

An adapter interface that converts business physical quantities to solver numeric types.

**amountToSolver($v$)** : Converts UInt64 amount to solver value.
**durationToSolver($v$)** : Converts duration physical quantity to solver value.
**costToSolver($v$)** : Converts cost physical quantity to solver value.

---

## 3. Variables

### 1. Decision Variables

#### Direct Compilation (CapacityCompilation)

**$x_{a,s}$** : Allocation quantity of production action $a$ at time slot $s$, non-negative integer variable, domain is $[0, upperBound(a, s, tw)]$, representing the number of batches/units, $\forall a \in A$, $\forall s \in S$.

**$cost$** : Total cost scalar, $cost = \sum_{a \in A} \sum_{s \in S} unitCost(a, s) \cdot x_{a,s}$.

#### Ordered Compilation (CapacityOrderCompilation)

**$x_{a,s,o}$** : Allocation quantity of production action $a$ at time slot $s$ and order position $o$, non-negative integer variable, domain is $[0, upperBound(a, s, tw)]$, representing the number of batches/units, $\forall a \in A$, $\forall s \in S$, $\forall o \in O$.

**$b_{a,s,o}$** : Binary variable indicating whether order position $o$ is occupied by production action $a$, domain is $\{0, 1\}$, $\forall a \in A$, $\forall s \in S$, $\forall o \in O$.

**$cost$** : Total cost scalar, $cost = \sum_{a \in A} \sum_{s \in S} \sum_{o \in O} unitCost(a, s) \cdot x_{a,s,o}$.

#### Iterative Compilation (IterativeCapacityCompilation)

**$x_{e,i,c}$** : Usage count of column $c$ in iteration $i$ for executor $e$, non-negative integer variable, domain is $[0, columnUpperBound(c)]$, $\forall e \in E$, $\forall i \in I_{iter}$, $\forall c \in C_{e,i}$.

**$cost$** : Total cost scalar, $cost = \sum_{e \in E} \sum_{i \in I_{iter}} \sum_{c \in C_{e,i}} columnCost(c) \cdot x_{e,i,c}$.

### 2. Auxiliary Variables

**$operationTime_{a,s}$** : Operation time expression representing the duration consumed by production action $a$ at time slot $s$, $operationTime_{a,s} = unitOpTime \cdot x_{a,s}$.

**$capacity_{e,s}$** : Capacity expression representing the total duration of executor $e$ at time slot $s$, $capacity_{e,s} = \sum_{a: executor(a)=e} operationTime_{a,s}$.

---

## 4. Predicates

### 1. Production Action Type Predicates

**discrete(a)** : Production action $a$ is in discrete batch-count mode, computing capacity and cost by batch quantity.
**continuous(a)** : Production action $a$ is in continuous duration-unit mode, where duration is determined by the time window interval.

### 2. Column Status Predicates

**isEmpty(c)** : Capacity column $c$ is empty, meaning its allocation map is empty or all allocation quantities are zero.

---

## 5. Sets

### 1. Production Actions

**$A$** : Universal set of all production actions.

**$A_{e}$** : Subset of production actions under executor $e$, i.e., actions with $executor(a) = e$, $\forall e \in E$.

### 2. Time Slots

**$S$** : Universal set of all time slots, obtained by discretizing the time window.

### 3. Executors

**$E$** : Universal set of all executors, derived from the executor property of production actions.

### 4. Order Positions

**$O$** : Universal set of all order positions, used only in the ordered variant, $O = \{0, 1, \ldots, maxOrderPerSlot - 1\}$.

### 5. Capacity Columns (Iterative Variant)

**$C_{e,i}$** : Set of capacity columns for executor $e$ in iteration $i$.

**$C_{e}^{rem}$** : Set of removed capacity columns for executor $e$.

---

## 6. Intermediate Values

### 1. Unit Operation Time

**Description**: The unit operation time of each production action, taking different values depending on whether the action is in discrete mode.

$$
unitOpTime_{a} = \begin{cases}
batchDuration_{a},& \text{discrete}(a) \\ \\
timeWindow.interval,& \neg \text{discrete}(a)
\end{cases}
$$

### 2. Operation Time

**Description**: The total duration consumed by a production action at a time slot, equal to the product of unit operation time and allocation quantity.

$$
operationTime_{a,s} = unitOpTime_{a} \cdot x_{a,s}, \; \forall a \in A, \; \forall s \in S
$$

### 3. Capacity

**Description**: The total duration consumed by each executor at each time slot, equal to the sum of operation times of all actions under that executor.

$$
capacity_{e,s} = \sum_{a \in A_{e}} operationTime_{a,s}, \; \forall e \in E, \; \forall s \in S
$$

### 4. Cost

**Description**: The total cost of all allocations, equal to the sum of the product of unit cost and allocation quantity for each action at each time slot.

$$
cost = \sum_{a \in A} \sum_{s \in S} unitCost(a, s.time.start) \cdot x_{a,s}
$$

### 5. Column Upper Bound

**Description**: The maximum number of times a capacity column can be used in a given time slot, equal to the floor of the slot duration divided by the column operation time.

$$
columnUpperBound(c) = \lfloor \frac{slotDuration(s)}{columnOperationTime(c)} \rfloor
$$

When the column operation time is zero or exceeds the slot duration, the upper bound returns 0.

### 6. Total Capacity Value

**Description**: The theoretical maximum total capacity across all actions and all time slots, used for assessing problem scale.

$$
totalCapacityValue = \sum_{a \in A} \sum_{s \in S} unitCapacity(a) \cdot upperBound(a, s)
$$

---

## 7. Assertions

### 1. Action Indexing Precondition

**Description**: Production actions must be indexed before model registration to ensure correct decision variable subscript mapping.

$$
\forall a \in A \; (indexed(a) \rightarrow registered(a))
$$

### 2. Column Data Integrity

**Description**: Capacity columns must pass data sanitization: the time slot index is within valid bounds, and all actions belong to the correct executor.

$$
\forall c \in C \; (slotIndex_{c} \in [0, |S|) \wedge \forall a \in allocations_{c} \; (executor_{a} = executor_{c}))
$$

### 3. Column Upper Bound Safety

**Description**: When a column's operation time exceeds or equals the slot duration, or is zero, the column upper bound returns zero.

$$
\forall c \in C \; (columnOperationTime(c) \geq slotDuration(s) \vee columnOperationTime(c) = 0 \rightarrow columnUpperBound(c) = 0)
$$

### 4. Order Constraint Precondition

**Description**: The order constraint requires that the decision variable $x_{a,t,o}$ has a finite upper bound.

$$
\forall a \in A, \forall t \in S, \forall o \in O \; (orderConstraintActive \rightarrow UB(a, t, o) < \infty)
$$

---

## 8. Constraints

### 1. Executor Capacity Constraint

**[CN]**: 执行器产能约束（硬约束）
**Description**: The total duration consumed by each executor at each time slot must not exceed the available duration of that time slot.

$$
s.t. \quad capacity_{e,s} \leq availableDuration(s), \; \forall e \in E, \; \forall s \in S
$$

### 2. Order Occupation Constraint C1

**[CN]**: 顺序占用约束 C1（硬约束，仅有序变体）
**Description**: At each order position of each time slot, at most one production action can be occupied.

$$
s.t. \quad \sum_{a \in A} b_{a,t,o} \leq 1, \; \forall t \in S, \; \forall o \in O
$$

### 3. Order Linking Constraint C2

**[CN]**: 顺序关联约束 C2（硬约束，仅有序变体）
**Description**: If an order position is occupied, the corresponding allocation quantity must be at least 1.

$$
s.t. \quad x_{a,t,o} \geq b_{a,t,o}, \; \forall a \in A, \; \forall t \in S, \; \forall o \in O
$$

### 4. Order Upper Bound Constraint C3

**[CN]**: 顺序上界约束 C3（硬约束，仅有序变体）
**Description**: If an order position is not occupied, the corresponding allocation quantity must be zero; if occupied, the allocation quantity must not exceed the upper bound.

$$
s.t. \quad x_{a,t,o} \leq UB(a,t,o) \cdot b_{a,t,o}, \; \forall a \in A, \; \forall t \in S, \; \forall o \in O
$$

---

## 9. Objective Function

### 1. Capacity Cost Minimization

**Description**: Minimize the total allocation cost across all production actions and all time slots.

$$
\min \; cost = \min \sum_{a \in A} \sum_{s \in S} unitCost(a, s.time.start) \cdot x_{a,s}
$$

Equivalent form for the iterative variant:

$$
\min \; cost = \min \sum_{e \in E} \sum_{i \in I_{iter}} \sum_{c \in C_{e,i}} columnCost(c) \cdot x_{e,i,c}
$$

---

## 10. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Column Generation | Built-in framework | Sections 3, 6 | RMP solving and column iteration mechanism for the iterative variant |
| Direct Compilation | Built-in framework | Section 3 | Direct modeling with 2D variable matrix |
| Ordered Compilation | Built-in framework | Section 3 | Ordered modeling with 3D variables and binary linking |

---

## 11. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 生产动作 | $a$ / ProductionAction | Production Action | A way to produce capacity; discrete or continuous |
| 执行器 | $e$ / Executor | Executor | A machine/resource performing actions |
| 时间槽 | $s$ / TimeSlot | Time Slot | A discretized time interval |
| 时间窗口 | $tw$ / TimeWindow | Time Window | Overall scheduling horizon |
| 产能列 | $c$ / CapacityColumn | Capacity Column | Allocation plan for one executor at one slot |
| 决策变量(数量) | $x_{a,s}$ / $x_{a,s,o}$ | Decision Variable (amount) | Integer: units/batches allocated |
| 决策变量(选择) | $b_{a,s,o}$ | Decision Variable (selection) | Binary: order position occupied |
| 操作时间 | $operationTime_{a,s}$ | Operation Time | Duration consumed by action in slot |
| 产能 | $capacity_{e,s}$ | Capacity | Total duration per executor-slot |
| 成本 | $cost$ | Cost | Total cost of all allocations |
| 单位成本 | $unitCost$ | Unit Cost | Cost per unit/batch |
| 单位产能 | $unitCapacity$ | Unit Capacity | Capacity produced per unit |
| 上界 | $upperBound$ | Upper Bound | Max units/batches per slot |
| 批次时长 | $batchDuration$ | Batch Duration | Fixed duration per batch (discrete) |
| 列聚合 | $CapacityColumnAggregation$ | Column Aggregation | Manages columns across iterations |
| 迭代 | $iteration$ | Iteration | A round of column generation |
| 顺序位置 | $order$ / $maxOrderPerSlot$ | Order Position | Sequential position within a slot |
| 求解器值适配器 | $schedulingSolverValueAdapter$ | Solver Value Adapter | Type conversion bridge |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Three compilation strategies | Single compilation model | Direct (2D) for simple cases, ordered (3D with binary linking) for order constraints, iterative/column generation for large-scale problems | 2024 |
| Generic numeric type V | Fixed Flt64 | Supports Flt64 and FltX, decoupling business domain from solver precision | 2024 |
| Column structural deduplication | Content-based comparison | Coroutine-parallelized structural comparison deduplication avoids generating semantically identical columns | 2024 |
| Dynamic variable resizing in iterative compilation | Fixed variable matrix | Dynamic variable extension during iteration supports incremental solving in column generation | 2024 |
| Separation of compilation and constraints/objectives | Unified compilation model | Separating compilation logic from constraint/objective definitions improves maintainability and strategy reuse | 2024 |
| Physical quantity type aliases | Direct use of solver types | Type aliases provide type-safe physical quantity wrapping while maintaining solver compatibility | 2024 |
| Column upper bound rounding strategy | Ceil / Direct division | Floor ensures slot capacity is not exceeded, consistent with physical constraints | 2024 |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial capacity scheduling model | Basic direct compilation framework |
| v1.1 | Added ordered compilation strategy | Support for order position constraints |
| v1.2 | Added iterative/column generation compilation | Support for incremental solving of large-scale problems |
| v1.3 | Generic numeric type V | Decoupling business domain from solver precision |
| v1.4 | Column aggregation and deduplication mechanism | Managing column lifecycle across iterations |
| v1.5 | Physical quantity type alias integration | Type-safe physical quantity wrapping |
