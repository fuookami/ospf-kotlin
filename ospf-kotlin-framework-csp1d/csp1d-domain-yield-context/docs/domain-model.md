# Yield Context Domain Model

[toc]

## 1. Overview

The Yield Context manages under-production and over-production slack variables for demands, quantifies the deviation between production and demands, and guides the solver to minimize deviation through penalty terms.

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

### 2. ProductDemand

Describes the production demand quantity for a specific product.

**$\text{product}_{d}$** : Product corresponding to demand $d$.
**$\text{quantity}_{d}$** : Demand value of demand $d$ (with physical unit).
**$\text{mode}_{d}$** : Demand mode label of demand $d$ (Roll/Weight/Sheet).

### 3. CuttingPlan

Describes a complete cutting operation, including material, slices, and demand contributions.

**$\text{id}_{p}$** : Unique identifier of plan $p$.
**$\text{material}_{p}$** : Material used by plan $p$.
**$\text{machineId}_{p}$** : Machine identifier used by plan $p$.
**$\text{slices}_{p}$** : Slice list of plan $p$.
**$\text{demandContributions}_{p}$** : Demand contribution list of plan $p$.
**$\text{capacityConsumption}_{p}$** : Machine capacity consumed by one usage of plan $p$.

### 4. CuttingPlanDemandContribution

Describes the contribution value of a cutting plan to a specific product demand.

**$\text{product}_{a}$** : Product corresponding to contribution $a$.
**$\text{quantity}_{a}$** : Contribution value of contribution $a$ (with physical unit).

---

## 3. Context Entities

### 1. UnderProduction

Describes the situation where product output is below demand.

**$\text{demand}_{u}$** : Demand corresponding to under-production $u$.
**$\text{shortfall}_{u}$** : Shortfall quantity of under-production $u$ (with physical unit).

### 2. OverProduction

Describes the situation where product output exceeds demand.

**$\text{demand}_{o}$** : Demand corresponding to over-production $o$.
**$\text{surplus}_{o}$** : Surplus quantity of over-production $o$ (with physical unit).

### 3. ProductOutput

Describes the total output quantity of a product.

**$\text{product}_{p}$** : Product corresponding to output $p$.
**$\text{totalQuantity}_{p}$** : Total output quantity of output $p$ (with physical unit).
**$\text{mode}_{p}$** : Demand mode label of output $p$.

### 4. YieldAnalysis

Complete result of yield deviation analysis.

**$\text{underProductions}_{a}$** : Under-production list of analysis $a$.
**$\text{overProductions}_{a}$** : Over-production list of analysis $a$.
**$\text{outputs}_{a}$** : Product output summary list of analysis $a$.

### 5. DemandAggregationKey

Aggregation key ensuring same-product different-unit outputs are not mixed.

**$\text{productId}_{k}$** : Product ID of aggregation key $k$.
**$\text{unit}_{k}$** : Physical unit of aggregation key $k$.

### 6. YieldModelingConfig

Yield modeling configuration consumed by the application layer.

**$\text{underProductionPenalty}_{c}$** : Under-production penalty coefficient mapping (by product+unit) in config $c$.
**$\text{overProductionPenalty}_{c}$** : Over-production penalty coefficient mapping (by product+unit) in config $c$.
**$\text{overProductionUpperBound}_{c}$** : Over-production upper bound mapping (by product+unit) in config $c$.

### 7. YieldModelingResult

Modeling result back-filled from solver solution.

**$\text{underProductions}_{r}$** : Under-production variable values list in result $r$.
**$\text{overProductions}_{r}$** : Over-production variable values list in result $r$.

### 8. ModeledUnderProduction

Flat modeling type recording solver variable values.

**$\text{productId}_{u}$** : Product ID of record $u$.
**$\text{unitSymbol}_{u}$** : Demand unit symbol of record $u$.
**$\text{amount}_{u}$** : Under-production amount of record $u$.

### 9. ModeledOverProduction

Flat modeling type recording solver variable values.

**$\text{productId}_{o}$** : Product ID of record $o$.
**$\text{unitSymbol}_{o}$** : Demand unit symbol of record $o$.
**$\text{amount}_{o}$** : Over-production amount of record $o$.

---

## 4. Variables

### 1. Decision Variables

This context does not directly define decision variables; they are managed by the Produce Context.

### 2. Auxiliary Variables

**$\text{under\_production}_{i}$** : Under-production slack variable for demand $i$, dimensionless normalized coefficient, domain is $[0, +\infty)$, records the portion where output is below demand, $\forall i \in D$.

**$\text{over\_production}_{i}$** : Over-production slack variable for demand $i$, dimensionless normalized coefficient, domain is $[0, +\infty)$, records the portion where output exceeds demand, $\forall i \in D$.

---

## 5. Predicates

### 1. Demand Predicates

**hasUnderPenalty** : Whether the demand defines under-production penalty.
**hasOverPenalty** : Whether the demand defines over-production penalty.
**hasOverBound** : Whether the demand defines over-production upper bound.
**needsOverSlackForOverArea** : Whether over-production slack is needed for over-production area penalty.

---

## 6. Sets

### 1. Under-Penalized Demand Set

**$D^{under}$** : Subset of demands with under-production penalty.

### 2. Over-Penalized Demand Set

**$D^{over}$** : Subset of demands with over-production penalty.

### 3. Over-Bounded Demand Set

**$D^{bound}$** : Subset of demands with over-production upper bound.

---

## 7. Intermediate Values

### 1. Total Output Quantity

**Description**: Total contribution quantity aggregated by product+unit.

$$
\text{totalOutput}_{i} = \sum_{j \in S} \text{contribution}_{ij} \times x_{j}
$$

### 2. Shortfall Quantity

**Description**: Demand quantity minus total output quantity when output is insufficient.

$$
\text{shortfall}_{i} = \text{demand}_{i} - \text{totalOutput}_{i}, \; \text{if totalOutput}_{i} < \text{demand}_{i}
$$

### 3. Surplus Quantity

**Description**: Total output quantity minus demand quantity when output exceeds demand.

$$
\text{surplus}_{i} = \text{totalOutput}_{i} - \text{demand}_{i}, \; \text{if totalOutput}_{i} > \text{demand}_{i}
$$

---

## 8. Assertions

### 1. Slack Variable Non-Negativity

**Description**: Under-production and over-production slack variables must be non-negative.

$$
\forall i \in D \; (\text{under\_production}_{i} \geq 0 \wedge \text{over\_production}_{i} \geq 0)
$$

### 2. Demand Aggregation Key Uniqueness

**Description**: Demands with the same product but different units are distinguished by aggregation keys.

$$
\forall i_1, i_2 \in D \; (i_1 \neq i_2 \rightarrow \text{key}_{i_1} \neq \text{key}_{i_2} \vee \text{unit}_{i_1} \neq \text{unit}_{i_2})
$$

---

## 9. Constraints

### 1. Demand Balance Constraint

**[CN]**：需求平衡约束
**Description**: The total contribution quantity plus under-production minus over-production equals the demand quantity.

$$
s.t. \quad \text{demandQuantity}_{i} + \text{under\_production}_{i} - \text{over\_production}_{i} = \text{demand}_{i}, \; \forall i \in D
$$

### 2. Over-Production Upper Bound Constraint

**[CN]**：超产上限约束
**Description**: The over-production slack variable must not exceed the configured over-production upper bound.

$$
s.t. \quad \text{over\_production}_{i} \leq \text{overProductionUpperBound}_{i}, \; \forall i \in D^{bound}
$$

---

## 10. Objective Function

**Description**: Minimize the weighted penalty of under-production and over-production.

$$
\min \sum_{i \in D} (\text{underPenalty}_{i} \times \text{under\_production}_{i} + \text{overPenalty}_{i} \times \text{over\_production}_{i})
$$

---

## 11. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Yield Deviation Analysis | - | Section 6 | Aggregate contributions by product+unit and compare with demands |

---

## 12. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Under-Production | $\text{under\_production}$ | Portion where output is below demand |
| Over-Production | $\text{over\_production}$ | Portion where output exceeds demand |
| Under-Production Penalty | $\text{underPenalty}$ | Penalty coefficient for under-production |
| Over-Production Penalty | $\text{overPenalty}$ | Penalty coefficient for over-production |
| Over-Production Upper Bound | $\text{overProductionUpperBound}$ | Upper bound for over-production |
| Yield Deviation | - | Difference between production and demand |
| Demand Aggregation Key | - | Key ensuring same-product different-unit outputs are not mixed |

---

## 13. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Aggregate output by product+unit | Aggregate by product only | Same-product different-unit outputs should not be mixed | - |
| Separate under-production and over-production slack variables | Single deviation variable | Can be independently penalized and constrained | - |
| Use dimensionless normalized coefficients | Quantities with units | Unit conversion responsibility delegated to caller | - |

---

## 14. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial version | Basic yield deviation management |
| v1.1 | Support aggregation by product+unit | Correct handling of same-product different-unit demands |
| v1.2 | Add over-production upper bound constraint | Over-production control requirements |
