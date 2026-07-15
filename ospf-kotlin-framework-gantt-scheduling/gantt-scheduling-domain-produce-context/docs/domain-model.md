# Produce Quantity Bounded Context — Domain Model

[toc]

## 1. Overview

The Produce Quantity context is responsible for managing the production quantity domain in Gantt scheduling. It tracks product output and raw material consumption for scheduled production tasks, enforces demand/reserve bounds with soft deviation relaxation, and supports both direct scheduling and column generation paradigms.

### 1. Dependent Contexts

1. **Gantt Scheduling Task Domain** (gantt-scheduling-domain-task-context) — AbstractTask, AbstractTaskBunch, Executor, AssignmentPolicy, AbstractMaterial
2. **Gantt Scheduling Capacity Scheduling Domain** (gantt-scheduling-domain-capacity-scheduling-context) — ProductionAction, CapacityColumn, Capacity, IterativeCapacityCompilation
3. **Gantt Scheduling Bunch Compilation Domain** (gantt-scheduling-domain-bunch-compilation-context) — BunchCompilation
4. **Gantt Scheduling Infrastructure** (gantt-scheduling-infrastructure) — TimeSlot, TimeWindow
5. **OSPF Core** (ospf-kotlin-core) — LinearMetaModel, LinearIntermediateSymbols, SlackFunction
6. **OSPF Framework** (ospf-kotlin-framework) — ShadowPrice, ShadowPriceKey

---

## 2. Concepts / Entities

### 1. Material

Abstract base class for any physical material. Subtypes: Product (finished good), SemiProduct (intermediate), RawMaterial (input material).

**$index_{m}$** : Index identifier of material $m$, integer.

### 2. MaterialDemand

Required output range for a product, describing the upper and lower bounds of the demanded output quantity and permitted deviations.

**$quantityRangeValue_{d}$** : Quantity range value of demand $d$, interval $[LB, UB]$, representing the lower and upper bounds of required output.
**$lessQuantityValue_{d}$** : Less-quantity deviation cap of demand $d$ (optional).
**$overQuantityValue_{d}$** : Over-quantity deviation cap of demand $d$ (optional).
**$lessEnabled_{d}$** : Whether demand $d$ enables less-quantity relaxation, boolean, true when $lessQuantityValue \neq \text{null}$.
**$overEnabled_{d}$** : Whether demand $d$ enables over-quantity relaxation, boolean, true when $overQuantityValue \neq \text{null}$.

### 3. MaterialReserves

Available input range for a material, describing the upper and lower bounds of consumable quantity and permitted deviations.

**$quantityRangeValue_{r}$** : Quantity range value of reserve $r$, interval $[LB, UB]$, representing the lower and upper bounds of available input.
**$lessQuantityValue_{r}$** : Less-quantity deviation cap of reserve $r$ (optional).
**$overQuantityValue_{r}$** : Over-quantity deviation cap of reserve $r$ (optional).
**$lessEnabled_{r}$** : Whether reserve $r$ enables less-quantity relaxation, boolean, true when $lessQuantityValue \neq \text{null}$.
**$overEnabled_{r}$** : Whether reserve $r$ enables over-quantity relaxation, boolean, true when $overQuantityValue \neq \text{null}$.

### 4. ProductionTask

A scheduled production task that produces products and consumes raw materials.

**$index_{t}$** : Index identifier of task $t$, integer.
**$id_{t}$** : Business identifier of task $t$.
**$name_{t}$** : Name of task $t$.
**$produceQuantityByProduct_{t}(p)$** : Unit output quantity of task $t$ for product $p$.
**$consumptionQuantityByMaterial_{t}(c)$** : Unit consumption quantity of task $t$ for material $c$.

### 5. Produce

Aggregate product output model, summarizing output across all tasks with deviation tracking.

**$products_{P}$** : Product set, sorted by index.
**$quantity_{P}(p)$** : Total produced amount for product $p$, continuous non-negative decision variable.
**$overQuantity_{P}(p)$** : Over-production slack for product $p$, continuous non-negative auxiliary variable.
**$lessQuantity_{P}(p)$** : Under-production slack for product $p$, continuous non-negative auxiliary variable.

### 6. Consumption

Aggregate material consumption model, summarizing consumption across all tasks with deviation tracking.

**$materials_{C}$** : Material set, sorted by index.
**$quantity_{C}(c)$** : Total consumed amount for material $c$, continuous non-negative decision variable.
**$overQuantity_{C}(c)$** : Over-consumption slack for material $c$, continuous non-negative auxiliary variable.
**$lessQuantity_{C}(c)$** : Under-consumption slack for material $c$, continuous non-negative auxiliary variable.

### 7. CapacityActionProduce

Unit production/consumption rates per production action.

**$produce_{a}(p)$** : Unit output rate of action $a$ for product $p$.
**$consumption_{a}(c)$** : Unit consumption rate of action $a$ for material $c$.

---

## 3. Variables

### 1. Decision Variables

**$quantity_{p}^{P}$** : Total produced amount for product $p$, continuous non-negative variable, domain is $[0, +\infty)$, representing the sum of output across all tasks for product $p$, $\forall p \in P$.

**$quantity_{c}^{C}$** : Total consumed amount for material $c$, continuous non-negative variable, domain is $[0, +\infty)$, representing the sum of consumption across all tasks for material $c$, $\forall c \in C$.

### 2. Auxiliary Variables

**$overQuantity_{p}^{P}$** : Over-production slack for product $p$, continuous non-negative variable, domain is $[0, +\infty)$, measuring the excess above the demand upper bound, $\forall p \in P$.

**$lessQuantity_{p}^{P}$** : Under-production slack for product $p$, continuous non-negative variable, domain is $[0, +\infty)$, measuring the shortfall below the demand lower bound, $\forall p \in P$.

**$overQuantity_{c}^{C}$** : Over-consumption slack for material $c$, continuous non-negative variable, domain is $[0, +\infty)$, measuring the excess above the reserve upper bound, $\forall c \in C$.

**$lessQuantity_{c}^{C}$** : Under-consumption slack for material $c$, continuous non-negative variable, domain is $[0, +\infty)$, measuring the shortfall below the reserve lower bound, $\forall c \in C$.

---

## 4. Predicates

### 1. Material Demand Predicates

**lessEnabled(d)** : Demand $d$ enables less-quantity relaxation, i.e., $lessQuantityValue_{d} \neq \text{null}$.
**overEnabled(d)** : Demand $d$ enables over-quantity relaxation, i.e., $overQuantityValue_{d} \neq \text{null}$.

### 2. Material Reserve Predicates

**lessEnabled(r)** : Reserve $r$ enables less-quantity relaxation, i.e., $lessQuantityValue_{r} \neq \text{null}$.
**overEnabled(r)** : Reserve $r$ enables over-quantity relaxation, i.e., $overQuantityValue_{r} \neq \text{null}$.

### 3. Produce / Consumption Predicates

**overEnabled(P)** : Produce model $P$ enables over-quantity tracking, depending on the corresponding demand's overEnabled.
**lessEnabled(P)** : Produce model $P$ enables less-quantity tracking, depending on the corresponding demand's lessEnabled.
**overEnabled(C)** : Consumption model $C$ enables over-quantity tracking, depending on the corresponding reserve's overEnabled.
**lessEnabled(C)** : Consumption model $C$ enables less-quantity tracking, depending on the corresponding reserve's lessEnabled.

### 4. Non-Zero Quantity Predicate

**isNonZero(q)** : Quantity value $q$ is non-zero, i.e., $q.value \neq 0$.

---

## 5. Sets

### 1. Products

**$P$** : Universal set of all products.

**$P^{demand}$** : Subset of products with demand constraints, i.e., products that have a MaterialDemand.
**$P^{nonZero}_{t}$** : Subset of products with non-zero produce quantity in task $t$, i.e., products satisfying $isNonZero(produceQuantityByProduct_{t}(p))$.

### 2. Raw Materials

**$C$** : Universal set of all raw materials.

**$C^{reserves}$** : Subset of materials with reserve constraints, i.e., materials that have a MaterialReserves.
**$C^{nonZero}_{t}$** : Subset of materials with non-zero consumption quantity in task $t$, i.e., materials satisfying $isNonZero(consumptionQuantityByMaterial_{t}(c))$.

---

## 6. Intermediate Values

### 1. Solver Lower Bound (solverLowerBound)

**Description**: Converts the range lower bound of a demand or reserve from business physical quantity to solver numeric value.

$$
solverLowerBound(d) = quantityRangeValue_{d}^{LB}.unwrap().toSolverValue()
$$

### 2. Solver Upper Bound (solverUpperBound)

**Description**: Converts the range upper bound of a demand or reserve from business physical quantity to solver numeric value.

$$
solverUpperBound(d) = quantityRangeValue_{d}^{UB}.unwrap().toSolverValue()
$$

### 3. Solver Less Quantity (solverLessQuantity)

**Description**: Converts the less-quantity deviation cap from business physical quantity to solver numeric value; defaults to 0 if not set.

$$
solverLessQuantity(d) = \begin{cases}
lessQuantityValue_{d}.toSolverValue(),& lessQuantityValue_{d} \neq \text{null} \\ \\
0,& lessQuantityValue_{d} = \text{null}
\end{cases}
$$

### 4. Solver Over Quantity (solverOverQuantity)

**Description**: Converts the over-quantity deviation cap from business physical quantity to solver numeric value; defaults to 0 if not set.

$$
solverOverQuantity(d) = \begin{cases}
overQuantityValue_{d}.toSolverValue(),& overQuantityValue_{d} \neq \text{null} \\ \\
0,& overQuantityValue_{d} = \text{null}
\end{cases}
$$

### 5. Solver Range Lower Bound (solverRangeLowerBound)

**Description**: Effective range lower bound after accounting for less-quantity deviation.

$$
solverRangeLowerBound(d) = solverLowerBound(d) - solverLessQuantity(d)
$$

### 6. Solver Range Upper Bound (solverRangeUpperBound)

**Description**: Effective range upper bound after accounting for over-quantity deviation.

$$
solverRangeUpperBound(d) = solverUpperBound(d) + solverOverQuantity(d)
$$

### 7. Bunch Produce (bunch.produce)

**Description**: Total output of a task bunch for product $p$, equal to the sum of all tasks' output quantities for that product.

$$
bunch.produce(p) = \sum_{t \in bunch.tasks} produceQuantityByProduct_{t}(p).value, \; \forall p \in P
$$

### 8. Bunch Consumption (bunch.consumption)

**Description**: Total consumption of a task bunch for material $c$, equal to the sum of all tasks' consumption quantities for that material.

$$
bunch.consumption(c) = \sum_{t \in bunch.tasks} consumptionQuantityByMaterial_{t}(c).value, \; \forall c \in C
$$

### 9. Capacity Column Produce (CapacityColumn.produce)

**Description**: Total output of a capacity column for product $p$ at given allocation amounts, equal to the sum of unit produce rates times allocation amounts across all allocations.

$$
CapacityColumn.produce(p, amountValue) = \sum_{(a, alloc) \in allocations} unitProduce(a, p) \cdot amountValue(alloc), \; \forall p \in P
$$

### 10. Capacity Column Consumption (CapacityColumn.consumption)

**Description**: Total consumption of a capacity column for material $c$ at given allocation amounts, equal to the sum of unit consumption rates times allocation amounts across all allocations.

$$
CapacityColumn.consumption(c, amountValue) = \sum_{(a, alloc) \in allocations} unitConsumption(a, c) \cdot amountValue(alloc), \; \forall c \in C
$$

---

## 7. Assertions

### 1. Product Index Ordering

**Description**: At construction of the Produce model, the product set $P$ must be sorted in ascending order by index.

$$
\forall i, j \in P \; (i < j \rightarrow index(P_i) \leq index(P_j))
$$

### 2. Material Index Ordering

**Description**: At construction of the Consumption model, the material set $C$ must be sorted in ascending order by index.

$$
\forall i, j \in C \; (i < j \rightarrow index(C_i) \leq index(C_j))
$$

### 3. Bunch Non-Empty

**Description**: In the addColumns operation, the task bunch must be non-empty, containing at least one task.

$$
|bunch.tasks| \geq 1
$$

### 4. Quantity Zero Prerequisite

**Description**: The quantityZero() method requires at least one ProductionTask with an available quantity value.

$$
\exists t \in Tasks \; (produceQuantityByProduct_{t}(p).value \neq \text{null})
$$

---

## 8. Constraints

### 1. Produce Upper Bound Constraint

**[CN]**: 产出上界约束
**Description**: For each product with a demand constraint, the total produced amount must not exceed the upper bound of the demand range. When over-quantity relaxation is enabled, the over-quantity symbol is used in place of the hard upper bound.

$$
s.t. \quad quantity_{p}^{P} \leq solverUpperBound(demand_{p}), \; \forall p \in P^{demand}
$$

**Corollary**: When over-quantity relaxation is enabled, the over-quantity variable is constrained:

$$
s.t. \quad overQuantity_{p}^{P} \cdot polyX \leq solverUpperBound(demand_{p}), \; \forall p \in P^{demand} \mid overEnabled(demand_{p})
$$

### 2. Produce Lower Bound Constraint

**[CN]**: 产出下界约束
**Description**: For each product with a demand constraint, the total produced amount must not fall below the lower bound of the demand range. When less-quantity relaxation is enabled, the less-quantity symbol is used in place of the hard lower bound.

$$
s.t. \quad quantity_{p}^{P} \geq solverLowerBound(demand_{p}), \; \forall p \in P^{demand}
$$

**Corollary**: When less-quantity relaxation is enabled, the less-quantity variable is constrained:

$$
s.t. \quad lessQuantity_{p}^{P} \cdot polyX \geq solverLowerBound(demand_{p}), \; \forall p \in P^{demand} \mid lessEnabled(demand_{p})
$$

### 3. Consumption Upper Bound Constraint

**[CN]**: 消耗上界约束
**Description**: For each material with a reserve constraint, the total consumed amount must not exceed the upper bound of the reserve range.

$$
s.t. \quad quantity_{c}^{C} \leq solverUpperBound(reserves_{c}), \; \forall c \in C^{reserves}
$$

### 4. Consumption Lower Bound Constraint

**[CN]**: 消耗下界约束
**Description**: For each material with a reserve constraint, the total consumed amount must not fall below the lower bound of the reserve range.

$$
s.t. \quad quantity_{c}^{C} \geq solverLowerBound(reserves_{c}), \; \forall c \in C^{reserves}
$$

### 5. Over-Quantity Cap Constraint

**[CN]**: 超量上限约束
**Description**: The over-quantity deviation for each product must not exceed the over-quantity cap permitted by the demand.

$$
s.t. \quad overQuantity_{p}^{P} \leq solverOverQuantity(demand_{p}), \; \forall p \in P^{demand}
$$

### 6. Less-Quantity Cap Constraint

**[CN]**: 欠量上限约束
**Description**: The less-quantity deviation for each product must not exceed the less-quantity cap permitted by the demand (negated to indicate the lower-bound direction).

$$
s.t. \quad lessQuantity_{p}^{P} \geq -solverLessQuantity(demand_{p}), \; \forall p \in P^{demand}
$$

---

## 9. Objective Function

### 1. Produce Quantity Minimization

**Description**: Minimize the total produced amount across all products, weighted by coefficients.

$$
\min \sum_{p \in P} coeff(p) \cdot quantity_{p}^{P}
$$

### 2. Produce Quantity Maximization

**Description**: Maximize the total produced amount across all products, weighted by coefficients.

$$
\max \sum_{p \in P} coeff(p) \cdot quantity_{p}^{P}
$$

### 3. Produce Less Quantity Minimization

**Description**: Minimize the sum of under-production deviations across all products, weighted by coefficients.

$$
\min \sum_{p \in P} coeff(p) \cdot lessQuantity_{p}^{P}
$$

### 4. Produce Over Quantity Minimization

**Description**: Minimize the sum of over-production deviations across all products, weighted by coefficients.

$$
\min \sum_{p \in P} coeff(p) \cdot overQuantity_{p}^{P}
$$

### 5. Consumption Quantity Minimization

**Description**: Minimize the total consumed amount across all materials, weighted by coefficients.

$$
\min \sum_{c \in C} coeff(c) \cdot quantity_{c}^{C}
$$

### 6. Consumption Quantity Maximization

**Description**: Maximize the total consumed amount across all materials, weighted by coefficients.

$$
\max \sum_{c \in C} coeff(c) \cdot quantity_{c}^{C}
$$

### 7. Consumption Less Quantity Minimization

**Description**: Minimize the sum of under-consumption deviations across all materials, weighted by coefficients.

$$
\min \sum_{c \in C} coeff(c) \cdot lessQuantity_{c}^{C}
$$

### 8. Consumption Over Quantity Minimization

**Description**: Minimize the sum of over-consumption deviations across all materials, weighted by coefficients.

$$
\min \sum_{c \in C} coeff(c) \cdot overQuantity_{c}^{C}
$$

> All objective functions support an optional threshold parameter for controlling the activation condition of secondary slack variables.

---

## 10. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Slack Function | Built-in (ospf-kotlin-core) | Section 8 Constraints, Section 9 Objectives | Creates positive/negative deviation slack variables |
| Column Generation | Built-in | Section 6 Intermediate Values | RMP solving and column iteration mechanism |
| Shadow Price Pipeline | Built-in (ospf-kotlin-framework) | Section 9 Objectives | Dual variable transmission architecture for CG pricing |

---

## 11. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 产品 | $P$ / $p$ | Product | A finished good produced by the system |
| 半成品 | - | Semi-Product | An intermediate material |
| 原材料 | $C$ / $c$ | Raw Material | An input material consumed |
| 物料 | $M$ | Material | Any physical item |
| 物料需求 | MaterialDemand | Material Demand | Required output range [LB,UB] with deviation caps |
| 物料储备 | MaterialReserves | Material Reserves | Available input range [LB,UB] with deviation caps |
| 生产任务 | ProductionTask | Production Task | A task that produces products and consumes materials |
| 产量 | Produce | Produce | Aggregate product output with deviation tracking |
| 消耗量 | Consumption | Consumption | Aggregate material consumption with deviation tracking |
| 数量 | quantity | Quantity | Total produced/consumed amount |
| 超量 | overQuantity | Over-Quantity | Slack measuring excess above upper bound |
| 欠量 | lessQuantity | Less-Quantity | Slack measuring shortfall below lower bound |
| 影子价格 | ShadowPrice | Shadow Price | Dual variable for CG pricing |
| 松弛函数 | SlackFunction | Slack Function | Creates positive/negative deviation variables |
| 流水线 | Pipeline | Pipeline | A constraint or objective in the model |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Dual-mode design: TaskScheduling (stub) and BunchScheduling (fully implemented) | Unified mode | TaskScheduling reserved for simple scenarios; BunchScheduling supports column generation iteration | 2024 |
| Capacity scheduling hierarchy: PlanCapacitySchedulingProduce (eager) vs BunchCapacitySchedulingProduce (lazy iterative) | Single strategy | Eager mode for direct scheduling; lazy iterative mode for CG scenarios | 2024 |
| Generic numeric type V | Fixed Flt64 | Generics support Flt64 and FltX, decoupling business domain from solver precision | 2024 |
| Lazy initialization of slack variables | Pre-allocate all slack variables | Create slack variables only when constraints are enabled, reducing model size | 2024 |
| Shadow price pipeline architecture | Direct dual value passing | Pipeline mode unifies pricing channels for constraints and objectives, supporting CG iteration | 2024 |
| Threshold-based secondary slack | No-threshold direct relaxation | Threshold controls activation of secondary slack, avoiding unnecessary variable inflation | 2024 |
| Immutable material lists sorted by index | Unordered lists | Sorting ensures deterministic traversal order and supports efficient operations such as binary search | 2024 |
| Bunch column rebuild strategy: clear and rebuild each iteration | Incremental update | Clear-and-rebuild avoids incremental maintenance complexity, ensuring iteration consistency | 2024 |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial production quantity model | Basic output/consumption tracking |
| v1.2 | Added MaterialDemand/MaterialReserves deviation relaxation | Support for soft constraint bounds |
| v1.3 | Added CapacityActionProduce capacity action model | Support for unit output/consumption rates in capacity columns |
| v1.4 | Added column generation support and shadow price pipeline | Support for pricing and iteration under CG paradigm |
| v1.5 | Generic numeric type V refactor | Decouple solver precision, support FltX |
| v1.6 | Threshold-based secondary slack and lazy initialization | Optimize model size and slack control |
