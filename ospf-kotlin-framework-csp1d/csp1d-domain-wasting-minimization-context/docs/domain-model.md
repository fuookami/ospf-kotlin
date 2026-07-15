# Wasting Minimization Context Domain Model

[toc]

## 1. Overview

The Wasting Minimization Context is responsible for analyzing and quantifying various wastes (rest width, rest material, over-production area) across cutting plan sets, and guiding the solver to minimize waste through penalty terms.

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

### 3. CuttingPlan

Describes a complete cutting operation, including material, slices, and demand contributions.

**$\text{id}_{p}$** : Unique identifier of plan $p$.
**$\text{material}_{p}$** : Material used by plan $p$.
**$\text{machineId}_{p}$** : Machine identifier used by plan $p$.
**$\text{slices}_{p}$** : Slice list of plan $p$.
**$\text{demandContributions}_{p}$** : Demand contribution list of plan $p$.
**$\text{capacityConsumption}_{p}$** : Machine capacity consumed by one usage of plan $p$.

### 4. ProductDemand

Describes the production demand quantity for a specific product.

**$\text{product}_{d}$** : Product corresponding to demand $d$.
**$\text{quantity}_{d}$** : Demand value of demand $d$ (with physical unit).
**$\text{mode}_{d}$** : Demand mode label of demand $d$ (Roll/Weight/Sheet).

---

## 3. Context Entities

### 1. RestWidthWaste

Records the remaining width waste of a cutting plan.

**$\text{plan}_{w}$** : Cutting plan corresponding to record $w$.
**$\text{restWidth}_{w}$** : Remaining width of record $w$ (with physical unit).

### 2. RestMaterialWaste

Records the rest material area proxy waste of a cutting plan.

**$\text{plan}_{m}$** : Cutting plan corresponding to record $m$.
**$\text{restMaterial}_{m}$** : Rest material area proxy of record $m$ (with physical unit).

### 3. OverProductionAreaWaste

Records the over-production area waste of a product.

**$\text{product}_{a}$** : Product corresponding to record $a$.
**$\text{wasteArea}_{a}$** : Waste area proxy of record $a$ (with physical unit).

### 4. WasteAnalysis

Waste analysis summary for a cutting plan set.

**$\text{restWidthWastes}_{a}$** : Rest width waste list of analysis $a$.
**$\text{restMaterialWastes}_{a}$** : Rest material waste list of analysis $a$.
**$\text{totalRestWidth}_{a}$** : Total rest width of analysis $a$.
**$\text{totalRestMaterial}_{a}$** : Total rest material area proxy of analysis $a$.

---

## 4. Variables

### 1. Decision Variables

This context does not directly define decision variables; they are managed by the Produce Context.

### 2. Auxiliary Variables

This context does not directly define auxiliary variables.

---

## 5. Predicates

### 1. Waste Predicates

**hasRestWidth** : Whether the cutting plan has remaining width waste.
**hasRestMaterial** : Whether the cutting plan has rest material waste.

---

## 6. Sets

### 1. Waste Plan Set

**$S^{waste}$** : Subset of plans with remaining width waste.

---

## 7. Intermediate Values

### 1. Rest Width

**Description**: Material upper bound width minus used width in a cutting plan.

$$
\text{restWidth}_{j} = \text{material}_{j}.\text{widthRange}.\text{upperBound} - \sum_{s \in \text{slices}_{j}} \text{width}_{s} \times \text{amount}_{s}
$$

### 2. Rest Material Area Proxy

**Description**: Rest width multiplied by material length, serving as a proxy for rest material area.

$$
\text{restMaterial}_{j} = \text{restWidth}_{j} \times \text{material}_{j}.\text{length}
$$

### 3. Total Rest Width

**Description**: Rest width of all selected plans accumulated by batch count.

$$
\text{totalRestWidth} = \sum_{j \in S} \text{restWidth}_{j} \times x_{j}
$$

### 4. Total Rest Material Area Proxy

**Description**: Rest material area proxy of all selected plans accumulated by batch count.

$$
\text{totalRestMaterial} = \sum_{j \in S} \text{restMaterial}_{j} \times x_{j}
$$

### 5. Over-Production Area Proxy

**Description**: Over-production quantity multiplied by product max width, serving as a proxy for over-production area.

$$
\text{overArea}_{i} = \text{over}_{i} \times \max(\text{width}_{i})
$$

---

## 8. Assertions

### 1. Rest Width Non-Negativity

**Description**: The remaining width of a cutting plan must be non-negative.

$$
\forall j \in S \; (\text{restWidth}_{j} \geq 0)
$$

---

## 9. Constraints

### 1. Demand Balance Constraint

**[CN]**：需求平衡约束
**Description**: The total contribution quantity for each demand must satisfy the demand (shared with Produce Context).

$$
s.t. \quad \text{demandQuantity}_{i} - \text{over}_{i} + \text{under}_{i} = \text{demand}_{i}, \; \forall i \in D
$$

---

## 10. Objective Function

**Description**: Minimize the weighted sum of various wastes.

$$
\min \sum_{j \in S} (\text{restWidth}_{j} \times \text{trimPenalty} + \text{restMaterial}_{j} \times \text{restPenalty} + \text{costPenalty}_{j}) \times x_{j} + \sum_{i \in D} \text{overArea}_{i} \times \text{overAreaPenalty}
$$

---

## 11. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Rest Width Calculation | - | Section 6 | Calculate remaining width from cutting plans |
| Rest Material Area Proxy | - | Section 6 | Rest width multiplied by material length |

---

## 12. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Rest Width | $\text{restWidth}$ | Material upper bound width minus used width |
| Rest Material | $\text{restMaterial}$ | Rest width multiplied by material length proxy |
| Over-Production Area | $\text{overArea}$ | Over-production quantity multiplied by product max width proxy |
| Trim Width Penalty | $\text{trimPenalty}$ | Penalty weight for rest width |
| Rest Material Penalty | $\text{restPenalty}$ | Penalty weight for rest material |
| Material Cost Penalty | $\text{costPenalty}$ | Per-material unit cost penalty |
| Over-Production Area Penalty | $\text{overAreaPenalty}$ | Penalty weight for over-production area |

---

## 13. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Use proxy values for rest material area | Exact calculation | Simplified computation, sufficiently accurate | - |
| Waste objective terms act directly on x variables | Additional slack variables | No extra variables needed, simplified model | - |
| Separate rest width and rest material penalties | Unified penalty | Different waste types can have independently adjustable weights | - |

---

## 14. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial version | Basic waste minimization functionality |
| v1.1 | Add over-production area penalty | Over-production waste quantification requirements |
| v1.2 | Add material cost penalty | Material cost optimization requirements |
