# Resource Context Domain Model — Gantt Scheduling

[toc]

## 1. Overview

The Resource Context models consumable resource capacity constraints with slack variables in gantt scheduling. It tracks how tasks consume shared resources over time — execution resources during tasks, connection resources during transitions, and storage resources in inventory — and enforces cumulative usage within capacity bounds. The context supports three scheduling modes: task-level, bunch (column generation), and capacity scheduling.

### 1. Dependent Contexts

1. **Gantt Scheduling Infrastructure** (gantt-scheduling-infrastructure) — TimeRange, TimeWindow, TimeSlot
2. **Gantt Scheduling Task Domain** (gantt-scheduling-domain-task-context) — Executor, AssignmentPolicy, AbstractTask, AbstractTaskBunch
3. **Gantt Scheduling Task Compilation** (gantt-scheduling-domain-task-compilation-context) — IterativeTaskCompilation
4. **Gantt Scheduling Bunch Compilation** (gantt-scheduling-domain-bunch-compilation-context) — BunchCompilation
5. **Gantt Scheduling Capacity Scheduling** (gantt-scheduling-domain-capacity-scheduling-context) — ProductionAction, Capacity, CapacityColumn, IterativeCapacityCompilation
6. **OSPF Core** (ospf-kotlin-core) — MetaModel, LinearIntermediateSymbols, SlackFunction
7. **OSPF Framework** (ospf-kotlin-framework) — ShadowPrice, ShadowPriceKey, Pipeline

---

## 2. Concepts / Entities

### 1. ResourceCapacity\<V\>

A time-bounded quantity range defining resource availability over a time interval. Each capacity specifies the allowed usage range, optional slack quantities, and the time period it governs.

**$time(C)$** : The time range during which capacity $C$ is active, a TimeRange value.
**$quantityRangeValue(C)$** : The allowed usage range $[LB, UB]$ of capacity $C$, a QuantityRange value.
**$lessQuantityValue(C)$** : Maximum permitted under-usage quantity of capacity $C$, a Quantity value (optional).
**$overQuantityValue(C)$** : Maximum permitted over-usage quantity of capacity $C$, a Quantity value (optional).
**$interval(C)$** : Duration of the time interval for capacity $C$.
**$name(C)$** : Human-readable name of capacity $C$.
**$lessEnabled(C)$** : Derived predicate; true when $lessQuantityValue(C)$ is defined and non-zero.
**$overEnabled(C)$** : Derived predicate; true when $overQuantityValue(C)$ is defined and non-zero.

### 2. Resource\<C, V\>

An entity with capacity that tasks consume. Resources are the central aggregate root, owning a collection of capacities and an initial quantity value. Subclasses specialize the consumption semantics.

**$id(R)$** : Unique identifier of resource $R$.
**$name(R)$** : Human-readable name of resource $R$.
**$capacities(R)$** : Collection of ResourceCapacity instances owned by resource $R$.
**$initialQuantityValue(R)$** : Starting quantity level of resource $R$ before any consumption.

**$usedQuantityQuantity(R, B, t, u)$** : Abstract method computing the total quantity consumed by bunch $B$ at time $t$ in unit $u$.

### 3. ExecutionResource\<C, V\>

A resource consumed during task execution. The usage is determined by the task's active time interval.

**$usedBy(R, T, t)$** : Abstract method returning the quantity of resource $R$ consumed by task $T$ at time $t$.

### 4. ConnectionResource\<C, V\>

A resource consumed during task transitions. The usage depends on the adjacency between consecutive tasks.

**$usedBy(R, T_{prev}, T_{next}, t)$** : Abstract method returning the quantity of resource $R$ consumed at time $t$ by the transition from $T_{prev}$ to $T_{next}$ (either may be null).

### 5. StorageResource\<C, V\>

A resource with supply and cost balance semantics, representing inventory that accumulates over time.

**$costBy(R, T, t)$** : Abstract method returning the cost quantity of resource $R$ consumed by task $T$ at time $t$.
**$supplyBy(R, T, t)$** : Abstract method returning the supply quantity of resource $R$ produced by task $T$ at time $t$.
**$fixedCostIn(R, t)$** : Concrete method returning the time-proportional fixed cost of resource $R$ at time $t$.
**$fixedSupplyIn(R, t)$** : Concrete method returning the time-proportional fixed supply of resource $R$ at time $t$.

### 6. ResourceTimeSlot\<R, C, V\>

A discretized time interval associated with a specific resource and capacity, serving as the fundamental unit for capacity constraint modeling.

**$origin(S)$** : The underlying TimeSlot from which this resource time slot is derived.
**$resource(S)$** : The resource to which this time slot belongs.
**$resourceCapacity(S)$** : The capacity governing this time slot.
**$time(S)$** : The time range covered by this time slot.
**$indexInRule(S)$** : The index of this time slot within the capacity rule.

**$relatedTo(S, T_{prev}, T_{next})$** : Predicate indicating whether the task pair $(T_{prev}, T_{next})$ has non-zero relation to time slot $S$.
**$relationTo(S, T_{prev}, T_{next})$** : Returns the quantitative relation coefficient between the task pair and time slot $S$.

### 7. ExecutionResourceTimeSlot

A ResourceTimeSlot specialized for execution resources. Inherits all properties and methods from ResourceTimeSlot.

### 8. ConnectionResourceTimeSlot

A ResourceTimeSlot specialized for connection resources. Inherits all properties and methods from ResourceTimeSlot.

### 9. StorageResourceTimeSlot

A ResourceTimeSlot specialized for storage resources. Inherits all properties and methods from ResourceTimeSlot with an additional time window reference.

**$timeWindow(S)$** : The TimeWindow associated with this storage resource time slot.

### 10. CapacityActionResource\<C, V\>

A resource variant used in capacity scheduling mode, where consumption is driven by production actions rather than tasks.

**$usedBy(R, A, t)$** : Abstract method returning the quantity of resource $R$ consumed by production action $A$ at time $t$.

### 11. ResourceUsage\<S, R, C, V\>

Tracks aggregate resource consumption across time slots, serving as the bridge between domain resources and solver decision variables.

**$name(U)$** : Name of this usage instance.
**$timeSlots(U)$** : Collection of resource time slots tracked by this usage.
**$quantity(U, s)$** : Decision variable for total resource usage at time slot $s$, $\forall s \in S$.
**$overQuantity(U, s)$** : Decision variable for over-usage slack at time slot $s$, $\forall s \in S$ (when overEnabled).
**$lessQuantity(U, s)$** : Decision variable for under-usage slack at time slot $s$, $\forall s \in S$ (when lessEnabled).
**$overEnabled(U)$** : Whether over-usage slack is enabled for this usage.
**$lessEnabled(U)$** : Whether under-usage slack is enabled for this usage.

### 12. ResourceCapacityShadowPriceKey

A shadow price key that uniquely identifies a resource capacity constraint for dual variable extraction in column generation.

---

## 3. Variables

### 1. Decision Variables

**$quantity_{s}$** : Continuous variable representing the total resource usage at time slot $s$, $\forall s \in S$. Initialized from the resource's initial quantity and accumulated task/bunch/action contributions.

**$overQuantity_{s}$** : Continuous non-negative variable representing the over-usage slack at time slot $s$, $\forall s \in S$. Active only when $overEnabled$ is true for the resource capacity.

**$lessQuantity_{s}$** : Continuous non-negative variable representing the under-usage slack at time slot $s$, $\forall s \in S$. Active only when $lessEnabled$ is true for the resource capacity.

**$supply_{r,t}$** : Continuous variable representing the cumulative supply of storage resource $r$ at time $t$, $\forall r \in R_{storage}$, $\forall t \in T$.

**$executorSupply_{e,r,t}$** : Continuous variable representing the per-executor supply contribution to storage resource $r$ at time $t$, $\forall e \in E$, $\forall r \in R_{storage}$, $\forall t \in T$.

**$cost_{r,t}$** : Continuous variable representing the cumulative cost of storage resource $r$ at time $t$, $\forall r \in R_{storage}$, $\forall t \in T$.

### 2. Auxiliary Variables

This context does not define auxiliary variables beyond the slack variables above.

---

## 4. Predicates

### 1. Capacity Slack Predicates

**lessEnabled(C)** : Resource capacity $C$ allows under-usage slack, i.e., $lessQuantityValue(C)$ is defined and non-zero.
**overEnabled(C)** : Resource capacity $C$ allows over-usage slack, i.e., $overQuantityValue(C)$ is defined and non-zero.

### 2. Time Slot Relation Predicates

**relatedTo(S, T_{prev}, T_{next})** : Time slot $S$ has non-zero relation to the task pair $(T_{prev}, T_{next})$, meaning the tasks contribute to resource consumption during this slot's time range.

---

## 5. Sets

### 1. Resources

**$R$** : Universal set of all resources.

**$R_{exec}$** : Subset of execution resources (ExecutionResource).
**$R_{conn}$** : Subset of connection resources (ConnectionResource).
**$R_{storage}$** : Subset of storage resources (StorageResource).
**$R_{capAction}$** : Subset of capacity action resources (CapacityActionResource).

### 2. Capacities

**$C_{r}$** : Set of all capacities owned by resource $r$, $\forall r \in R$.

### 3. Time Slots

**$S$** : Universal set of all discretized resource time slots.

**$S_{r}$** : Subset of time slots associated with resource $r$, $\forall r \in R$.
**$S_{c}$** : Subset of time slots governed by capacity $c$, $\forall c \in C_{r}$.

### 4. Executors

**$E$** : Set of all executors, relevant for storage resource supply computation.

### 5. Production Actions

**$A$** : Set of all production actions, relevant for capacity scheduling mode.

### 6. Bunches

**$B$** : Set of all task bunches (columns) in column generation.

---

## 6. Intermediate Values

### 1. Solver Lower Bound

**Description**: The lower bound of the resource capacity quantity range converted to solver numeric value.

$$
solverLowerBound_{c} = quantityRangeValue(c).lowerBound.value.unwrap().toSolverValue()
$$

### 2. Solver Upper Bound

**Description**: The upper bound of the resource capacity quantity range converted to solver numeric value.

$$
solverUpperBound_{c} = quantityRangeValue(c).upperBound.value.unwrap().toSolverValue()
$$

### 3. Solver Less Quantity

**Description**: The maximum permitted under-usage quantity converted to solver numeric value.

$$
solverLessQuantity_{c} = lessQuantityValue(c).value.toSolverValue() \quad (0 \text{ if null})
$$

### 4. Solver Over Quantity

**Description**: The maximum permitted over-usage quantity converted to solver numeric value.

$$
solverOverQuantity_{c} = overQuantityValue(c).value.toSolverValue() \quad (0 \text{ if null})
$$

### 5. Solver Value Range

**Description**: The effective capacity range after accounting for slack quantities.

$$
solverValueRange_{c} = [solverLowerBound_{c} - solverLessQuantity_{c}, \; solverUpperBound_{c} + solverOverQuantity_{c}]
$$

### 6. Solver Initial Quantity

**Description**: The resource's initial quantity converted to solver numeric value.

$$
solverInitialQuantity_{r} = initialQuantityValue(r).value.toSolverValue()
$$

### 7. Execution Quantity

**Description**: For execution resources, the total quantity at a time slot is the initial quantity plus the sum of all task contributions within that slot's time range.

$$
quantity_{s} = solverInitialQuantity_{r} + \sum_{T \in Tasks} usedBy(r, T, time(s)), \; \forall s \in S_{r}, \; r \in R_{exec}
$$

### 8. Storage Quantity

**Description**: For storage resources, the total quantity at a time slot is the initial quantity plus cumulative supply minus cumulative cost.

$$
quantity_{s} = solverInitialQuantity_{r} + supply_{r,t} - cost_{r,t}, \; \forall s \in S_{r}, \; r \in R_{storage}
$$

### 9. Total Supply

**Description**: The total supply for a storage resource combines fixed supply and per-executor contributions.

$$
supply_{r,t} = fixedSupplyIn(r, t) + \sum_{e \in E} executorSupply_{e,r,t}, \; \forall r \in R_{storage}, \; \forall t \in T
$$

---

## 7. Assertions

### 1. Non-Empty Bunches

**Description**: When adding columns via bunch-based methods, the bunches collection must not be empty.

$$
|B| > 0 \quad \text{(in addColumns for bunch variants)}
$$

### 2. Non-Empty Tasks

**Description**: When adding columns via task-based methods, the tasks collection must not be empty.

$$
|Tasks| > 0 \quad \text{(in addColumns for task variants)}
$$

### 3. Capacity Bounds Validity

**Description**: Resource capacity bounds must contain at least one finite bound (either lower or upper).

$$
\forall c \in C \; (|LB(c)| < \infty \vee |UB(c)| < \infty)
$$

### 4. Time Slot Generation Validity

**Description**: The begin time of a time slot must be strictly less than the end time.

$$
\forall s \in S \; (beginTime(s) < endTime(s))
$$

---

## 8. Constraints

### 1. Resource Capacity Upper Bound

**[CN]**: 资源容量上界约束
**Description**: The total resource usage at each time slot must not exceed the upper bound of the governing capacity. When slack relaxation is enabled and over-usage is permitted, the over-quantity slack variable absorbs the violation.

$$
s.t. \quad quantity_{s} \leq solverUpperBound_{c(s)}, \; \forall s \in S
$$

With slack relaxation ($withSlack \wedge overEnabled$):

$$
s.t. \quad quantity_{s} - overQuantity_{s} \cdot polyX \leq solverUpperBound_{c(s)}, \; \forall s \in S
$$

Tagged with $ResourceCapacityShadowPriceKey(s)$ for dual variable extraction.

### 2. Resource Capacity Lower Bound

**[CN]**: 资源容量下界约束
**Description**: The total resource usage at each time slot must not fall below the lower bound of the governing capacity. When slack relaxation is enabled and under-usage is permitted, the less-quantity slack variable absorbs the violation.

$$
s.t. \quad quantity_{s} \geq solverLowerBound_{c(s)}, \; \forall s \in S
$$

With slack relaxation ($withSlack \wedge lessEnabled$):

$$
s.t. \quad quantity_{s} + lessQuantity_{s} \cdot polyX \geq solverLowerBound_{c(s)}, \; \forall s \in S
$$

Tagged with $ResourceCapacityShadowPriceKey(s)$ for dual variable extraction.

---

## 9. Objective Function

### 1. Resource Over-Quantity Minimization

**Description**: Minimize the total over-usage slack across all time slots, penalizing resource capacity violations. Supports threshold-based slack weighting.

$$
\min \sum_{s \in S} coeff(s) \cdot overQuantity_{s}
$$

### 2. Resource Less-Quantity Minimization

**Description**: Minimize the total under-usage slack across all time slots, penalizing resource capacity underutilization. Supports threshold-based slack weighting.

$$
\min \sum_{s \in S} coeff(s) \cdot lessQuantity_{s}
$$

---

## 10. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Column Generation | ospf-kotlin-core | Sections 3, 6, 8, 9 | RMP solving and column iteration for bunch/capacity scheduling modes |
| Slack Function | ospf-kotlin-core | Section 8 | Decomposes constraint violations into positive/negative slack components |
| Shadow Price Pipeline | ospf-kotlin-framework | Section 8 | Unified constraint addition and dual variable extraction for CG pricing |
| Coroutine Parallelism | Kotlin Coroutines | Section 6 (storage) | Parallel supply/cost computation for storage resources |

---

## 11. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 资源 | $R$ / Resource\<C,V\> | Resource | An entity with capacity that tasks consume |
| 容量 | $C$ / ResourceCapacity\<V\> | Capacity | A time-bounded quantity range for resource availability |
| 数量范围 | QuantityRange\<V\> | Quantity Range | The [LB,UB] interval of allowed usage |
| 欠量 | lessQuantityValue | Less Quantity | Maximum permitted under-usage |
| 超量 | overQuantityValue | Over Quantity | Maximum permitted over-usage |
| 初始数量 | initialQuantityValue | Initial Quantity | Starting resource level before consumption |
| 时间槽 | $s$ / ResourceTimeSlot\<R,C,V\> | Time Slot | Discretized time interval for a resource |
| 时间窗口 | TimeWindow\<V\> | Time Window | Scheduling horizon with discretization |
| 使用量 | $U$ / ResourceUsage\<S,R,C,V\> | Usage | Aggregate resource consumption across time slots |
| 数量 | $quantity_{s}$ | Quantity | LP variable for total resource usage at a slot |
| 松弛 | SlackFunction | Slack | Decomposes constraint violation into positive/negative parts |
| 影子价格 | ShadowPrice | Shadow Price | Dual variable for CG pricing |
| 执行器供给 | $executorSupply_{e,r,t}$ | Executor Supply | Per-executor resource contribution (storage) |
| 固定成本 | $fixedCostIn(r,t)$ | Fixed Cost | Time-proportional fixed resource cost |
| 固定供给 | $fixedSupplyIn(r,t)$ | Fixed Supply | Time-proportional fixed resource supply |
| 生产动作 | $A$ / ProductionAction | Production Action | A manufacturing action consuming resources |
| 列 | $B$ / CapacityColumn / AbstractTaskBunch | Column | A candidate schedule pattern for CG |
| 编排 | Compilation | Compilation | Accumulates column variables across iterations |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Three resource archetypes (Execution, Connection, Storage) | Unified resource model | Different consumption semantics (during execution, during transition, supply/cost balance) require distinct abstractions | 2025 |
| Solver value boundary conversion (domain V to Flt64) | Direct solver use of domain types | Boundary conversion isolates solver numeric concerns from domain model, enabling unit-agnostic resource definitions | 2025 |
| Lazy variable initialization (lateinit in register) | Eager initialization | Deferred initialization avoids premature solver allocation and supports iterative column generation workflows | 2025 |
| Column generation support via addColumns methods | Single monolithic registration | Separate addColumns variants for bunch, task, and capacity scheduling enable flexible CG integration per mode | 2025 |
| Task-level scheduling stubs return Failed | Full task-level implementation | Task-level scheduling is deferred; stubs provide clear failure signals without blocking other modes | 2025 |
| Coroutine parallelism for storage supply/cost | Sequential computation | Storage resources involve independent per-executor computations that benefit from parallel execution | 2025 |
| Shadow price pipeline for constraint/dual coupling | Manual dual extraction | Unified pipeline automates constraint addition and dual variable tagging, reducing boilerplate in CG iterations | 2025 |
| CapacityActionResource for ProductionAction | Reusing existing resource types | Production actions in capacity scheduling have distinct consumption patterns requiring a dedicated resource variant | 2025 |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial resource context model | Establish consumable resource capacity framework with three archetypes |
| v1.1 | Added slack variable support (over/less quantity) | Enable soft constraint relaxation for capacity bounds |
| v1.2 | Column generation integration (addColumns) | Support bunch and capacity scheduling CG modes |
| v1.3 | Storage resource supply/cost balance | Model inventory-style resources with accumulation semantics |
| v1.4 | CapacityActionResource for capacity scheduling | Integrate production action resource consumption |
| v1.5 | Shadow price pipeline integration | Automate dual variable extraction for CG pricing |
