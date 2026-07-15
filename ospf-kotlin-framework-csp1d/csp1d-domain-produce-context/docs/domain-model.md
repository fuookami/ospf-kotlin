# Produce Context Domain Model

[toc]

## 1. Overview

The Produce Context manages the core decision variables (cutting plan batch counts) and constraint intermediate symbols of the CSP1D master problem, supporting MILP registration and column generation iterative mode.

---

## 2. Base Entities

### 1. Product

Describes material specification properties; the target output of cutting production.

**$\text{id}_{i}$** : Unique identifier of product $i$.
**$\text{name}_{i}$** : Name of product $i$.
**$\text{width}_{i}$** : List of widths for product $i$, supporting multiple specifications.
**$\text{length}_{i}$** : Length of product $i$; null for dynamic-length products.
**$\text{unitWeight}_{i}$** : Unit weight of product $i$.
**$\text{maxOverProduceLength}_{i}$** : Maximum overproduce length of product $i$ for length assignment constraints.
**$\text{dynamicLength}_{i}$** : Whether product $i$ is dynamic-length; if true, length and weight must be null.

### 2. Material

The raw material for cutting production; describes the parent roll specifications available for cutting.

**$\text{id}_{m}$** : Unique identifier of material $m$.
**$\text{name}_{m}$** : Name of material $m$.
**$\text{widthRange}_{m}$** : Available width range of material $m$, including bounds and step.
**$\text{length}_{m}$** : Coil length of material $m$.
**$\text{unitWeight}_{m}$** : Unit weight of material $m$.
**$\text{machineId}_{m}$** : Machine identifier bound to material $m$; null means no machine restriction.
**$\text{availableBatches}_{m}$** : Available batch count of material $m$; modeled by plan usage sum in master problem.

### 3. Machine

Cutting production equipment; constrains processable materials and capacity.

**$\text{id}_{k}$** : Unique identifier of machine $k$.
**$\text{name}_{k}$** : Name of machine $k$.
**$\text{maxBatchCount}_{k}$** : Maximum batch count of machine $k$.
**$\text{maxSwitchCount}_{k}$** : Maximum material switch count of machine $k$; not modeled in current unordered master problem.
**$\text{widthRange}_{k}$** : Processable width range of machine $k$.
**$\text{capacity}_{k}$** : Business capacity upper bound of machine $k$; modeled only with same-unit plan capacity consumption.

### 4. CuttingPlan

Describes a complete cutting operation, including material, slices, and demand contributions.

**$\text{id}_{p}$** : Unique identifier of plan $p$.
**$\text{material}_{p}$** : Material used by plan $p$.
**$\text{machineId}_{p}$** : Machine identifier used by plan $p$.
**$\text{slices}_{p}$** : Slice list of plan $p$.
**$\text{demandContributions}_{p}$** : Demand contribution list of plan $p$.
**$\text{capacityConsumption}_{p}$** : Machine capacity consumed by one usage of plan $p$.

### 5. CuttingPlanSlice

Describes the production target and width in a single cut.

**$\text{production}_{s}$** : Production target (product or costar) of slice $s$.
**$\text{width}_{s}$** : Width of slice $s$.
**$\text{amount}_{s}$** : Piece amount of slice $s$.

### 6. Costar

Byproduct that can fill remaining width in cutting plans.

**$\text{id}_{c}$** : Unique identifier of costar $c$.
**$\text{name}_{c}$** : Name of costar $c$.
**$\text{width}_{c}$** : Width list of costar $c$.
**$\text{length}_{c}$** : Length of costar $c$.
**$\text{unitWeight}_{c}$** : Unit weight of costar $c$.

### 7. ProductDemand

Describes the production demand quantity for a specific product.

**$\text{product}_{d}$** : Product corresponding to demand $d$.
**$\text{quantity}_{d}$** : Demand value of demand $d$ (with physical unit).
**$\text{mode}_{d}$** : Demand mode label of demand $d$ (Roll/Weight/Sheet).

### 8. WidthRange

Width value range with step and unit consistency.

**$\text{width}_{w}$** : Value range of width range $w$ (QuantityRange).
**$\text{step}_{w}$** : Step value of width range $w$.

### 9. CuttingPlanDemandContribution

Describes the contribution value of a cutting plan to a specific product demand.

**$\text{product}_{a}$** : Product corresponding to contribution $a$.
**$\text{quantity}_{a}$** : Contribution value of contribution $a$ (with physical unit).

---

## 3. Context Entities

### 1. CuttingPlanUsage

Describes the usage amount of a cutting plan.

**$\text{plan}_{u}$** : Cutting plan corresponding to usage $u$.
**$\text{amount}_{u}$** : Batch count of usage $u$.

### 2. MaterialUsage

Describes the usage batch count of a material.

**$\text{material}_{m}$** : Material corresponding to usage $m$.
**$\text{amount}_{m}$** : Batch count of usage $m$.

### 3. MachineCapacityUsage

Describes the capacity usage of a machine.

**$\text{machine}_{c}$** : Machine corresponding to usage $c$.
**$\text{used}_{c}$** : Actual used capacity of usage $c$ (aggregated from plan consumption).

### 4. Produce

Complete output of the master problem solution.

**$\text{cuttingPlans}_{p}$** : Selected cutting plans in output $p$.
**$\text{materialUsages}_{p}$** : Material usage statistics in output $p$.
**$\text{machineUsages}_{p}$** : Machine capacity statistics in output $p$.
**$\text{unmetDemands}_{p}$** : Unmet demands in output $p$.

### 5. ContributionKey

Aggregation key ensuring same-product different-unit contributions are not mixed.

**$\text{productId}_{k}$** : Product ID of aggregation key $k$.
**$\text{unit}_{k}$** : Physical unit of aggregation key $k$.

---

## 4. Variables

### 1. Decision Variables

**$x_{j}$** : Batch count of cutting plan $j$, integer variable, domain is $\{0, 1, 2, \ldots\}$, represents the number of times plan $j$ is executed, $\forall j \in S$.

### 2. Auxiliary Variables

This context does not directly define auxiliary variables; they are managed by the Length Assignment Context and Yield Context.

---

## 5. Predicates

### 1. Plan Predicates

**hasDemandContribution** : Whether the plan contributes to a specific demand.
**isAssignedToMachine** : Whether the plan is assigned to a specific machine.

---

## 6. Sets

### 1. Cutting Plan Set

**$S$** : Universal set of all cutting plans.

**$S_{m}$** : Subset of plans using material $m$.
**$S_{k}$** : Subset of plans using machine $k$.

### 2. Iteration Plan Set

**$S^{(t)}$** : Subset of plans added in iteration $t$.

---

## 7. Intermediate Values

### 1. Demand Contribution Quantity

**Description**: Total contribution quantity for each demand, aggregated from plan usage and contribution coefficients.

$$
\text{demandQuantity}_{i} = \sum_{j \in S} \text{contribution}_{ij} \times x_{j}, \; \forall i \in D
$$

where $\text{contribution}_{ij}$ is the contribution coefficient of plan $j$ to demand $i$.

### 2. Material Usage Quantity

**Description**: Total usage quantity for each material.

$$
\text{materialQuantity}_{m} = \sum_{j \in S_{m}} x_{j}, \; \forall m \in M
$$

### 3. Machine Batch Quantity

**Description**: Total batch count for each machine.

$$
\text{machineBatchQuantity}_{k} = \sum_{j \in S_{k}} x_{j}, \; \forall k \in K
$$

### 4. Machine Capacity Quantity

**Description**: Total capacity consumption for each machine.

$$
\text{machineCapacityQuantity}_{k} = \sum_{j \in S_{k}} \text{consumption}_{j} \times x_{j}, \; \forall k \in K
$$

---

## 8. Assertions

### 1. Plan Uniqueness

**Description**: Each cutting plan has a unique identifier.

$$
\forall j_1, j_2 \in S \; (j_1 \neq j_2 \rightarrow \text{id}_{j_1} \neq \text{id}_{j_2})
$$

### 2. Column Generation Deduplication

**Description**: New plans are deduplicated by id and canonicalKey.

$$
\forall j \in S^{(t)} \; (\text{id}_{j} \notin \text{registeredIds} \wedge \text{canonicalKey}_{j} \notin \text{registeredKeys})
$$

---

## 9. Constraints

### 1. Demand Balance Constraint

**[CN]**：需求平衡约束
**Description**: The total contribution quantity for each demand must satisfy the demand (equality with yield slack, inequality without).

With yield slack:

$$
s.t. \quad \text{demandQuantity}_{i} - \text{over}_{i} + \text{under}_{i} = \text{demand}_{i}, \; \forall i \in D
$$

Without yield slack:

$$
s.t. \quad \text{demandQuantity}_{i} \geq \text{demand}_{i}, \; \forall i \in D
$$

### 2. Material Available Batch Constraint

**[CN]**：物料可用批次约束
**Description**: The total usage of each material must not exceed its available batch count.

$$
s.t. \quad \text{materialQuantity}_{m} \leq \text{availableBatches}_{m}, \; \forall m \in M
$$

### 3. Machine Batch Count Constraint

**[CN]**：设备批次数约束
**Description**: The total plan usage assigned to each machine must not exceed its maximum batch count.

$$
s.t. \quad \text{machineBatchQuantity}_{k} \leq \text{maxBatchCount}_{k}, \; \forall k \in K
$$

### 4. Machine Capacity Constraint

**[CN]**：设备产能约束
**Description**: The total capacity consumption assigned to each machine must not exceed its capacity limit.

$$
s.t. \quad \text{machineCapacityQuantity}_{k} \leq \text{capacity}_{k}, \; \forall k \in K
$$

---

## 10. Objective Function

**Description**: Minimize total batch usage, with optional extra weight via batchCoefficient.

$$
\min \sum_{j \in S} \text{batchCoefficient} \times x_{j}
$$

---

## 11. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Column Generation | - | Section 5 | Plan management during column generation iteration |

---

## 12. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Batch Count | $x$ | Number of times a cutting plan is executed |
| Demand Quantity | $\text{demandQuantity}$ | Total contribution quantity for each demand |
| Material Quantity | $\text{materialQuantity}$ | Total usage quantity for each material |
| Machine Batch Quantity | $\text{machineBatchQuantity}$ | Total batch count for each machine |
| Machine Capacity Quantity | $\text{machineCapacityQuantity}$ | Total capacity consumption for each machine |
| Intermediate Symbol | - | Intermediate expression referenced by constraint pipelines |
| Column Generation | - | Iterative solving method adding new plans |

---

## 13. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Reference constraint pipelines via intermediate symbols | Direct x variable reference | addColumns only needs to flush intermediate symbols, not refresh constraints | - |
| Use canonicalKey for deduplication | ID-only deduplication | Prevent semantically identical plans with different IDs from being added | - |
| Support column generation iterative mode | Single registration mode | Support iterative solving for large-scale problems | - |

---

## 14. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial version | Master problem core variable and constraint management |
| v1.1 | Column generation iterative mode support | Large-scale problem solving requirements |
| v1.2 | Intermediate symbol mechanism | Simplify constraint refresh logic during addColumns |
