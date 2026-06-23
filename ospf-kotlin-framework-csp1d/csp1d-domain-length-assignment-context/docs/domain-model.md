# Length Assignment Context Domain Model

[toc]

## 1. Overview

The Length Assignment Context is responsible for dynamic coil length assignment and over-length detection, deriving optimal coil lengths for dynamic-length products and detecting over-production constraint violations.

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

---

## 3. Context Entities

### 1. LengthAssignment

Describes the assigned coil length result for a single product.

**$\text{product}_{a}$** : Product corresponding to assignment $a$.
**$\text{assignedLength}_{a}$** : Assigned coil length of assignment $a$ (with physical unit).
**$\text{batchCount}_{a}$** : Batch count of assignment $a$.

### 2. OverLengthRecord

Records the portion of a product's actual coil length exceeding the maximum overproduce length.

**$\text{product}_{r}$** : Product corresponding to record $r$.
**$\text{overLength}_{r}$** : Over-length quantity of record $r$ (with physical unit).

### 3. LengthAssignmentModelingConfig

Length assignment configuration consumed by the application layer.

**$\text{dynamicProductIds}_{c}$** : Set of product IDs requiring dynamic length assignment in config $c$.
**$\text{assignedLengthLowerBound}_{c}$** : Assigned length lower bound mapping (by product ID) in config $c$.
**$\text{assignedLengthUpperBound}_{c}$** : Assigned length upper bound mapping (by product ID) in config $c$.
**$\text{overLengthPenalty}_{c}$** : Over-length penalty weight mapping (by product ID) in config $c$.
**$\text{overLengthUpperBound}_{c}$** : Over-length upper bound mapping (by product ID) in config $c$.
**$\text{totalLengthPenalty}_{c}$** : Total length penalty weight in config $c$.
**$\text{batchMinPenalty}_{c}$** : Batch minimization penalty weight in config $c$.

### 4. LengthAssignmentModelingResult

Modeling result back-filled from solver solution.

**$\text{assignedLengths}_{r}$** : List of assigned length values for dynamic-length products in result $r$.
**$\text{overLengths}_{r}$** : List of over-length values in result $r$.

### 5. ModeledAssignedLength

Flat modeling type recording solver variable values.

**$\text{productId}_{l}$** : Product ID of record $l$.
**$\text{assignedLength}_{l}$** : Assigned length value of record $l$.

### 6. ModeledOverLength

Flat modeling type recording solver variable values.

**$\text{productId}_{o}$** : Product ID of record $o$.
**$\text{overLength}_{o}$** : Over-length value of record $o$.

---

## 4. Variables

### 1. Decision Variables

**$\text{assigned\_length}_{i}$** : Assigned coil length of product $i$, dimensionless normalized coefficient, domain is $[0, +\infty)$, decision coil length for dynamic-length products, $\forall i \in P^{dyn}$.

### 2. Auxiliary Variables

**$\text{over\_length}_{i}$** : Over-length slack variable of product $i$, dimensionless normalized coefficient, domain is $[0, +\infty)$, records the portion exceeding maximum overproduce length, $\forall i \in P^{dyn}$.

---

## 5. Predicates

### 1. Product Predicates

**isDynamic** : Whether the product is in the dynamic product ID set.
**hasBound** : Whether the product defines length upper/lower bounds.
**hasPenalty** : Whether the product defines total length penalty or over-length penalty.

---

## 6. Sets

### 1. Dynamic Product Set

**$P^{dyn}$** : Set of product IDs requiring dynamic length assignment, defined by LengthAssignmentModelingConfig.dynamicProductIds.

### 2. Bounded Product Set

**$P^{bound}$** : Subset of products with defined length bounds.

### 3. Penalized Product Set

**$P^{penalty}$** : Subset of products with over-length or total length penalties.

---

## 7. Intermediate Values

### 1. Derived Length

**Description**: Coil length value computed by the length derivation function injected by the downstream project.

$$
\text{derivedLength}_{i} = \text{LengthDerivation}(\text{demandQuantity}_{i}, \text{product}_{i})
$$

Its definition is further specified in *Length Derivation Function*.

---

## 8. Assertions

### 1. Dynamic Product Prerequisite

**Description**: Only dynamic-length products have length assignment variables.

$$
\forall i \in P \; (\text{assigned\_length}_{i} \neq \text{null} \rightarrow \text{isDynamic}_{i})
$$

---

## 9. Constraints

### 1. Assigned Length Lower Bound Constraint

**[CN]**：卷长下界约束
**Description**: The assigned length of dynamic-length products must not be below the configured lower bound.

$$
s.t. \quad \text{assigned\_length}_{i} \geq \text{lowerBound}_{i}, \; \forall i \in P^{bound}
$$

### 2. Assigned Length Upper Bound Constraint

**[CN]**：卷长上界约束
**Description**: The assigned length of dynamic-length products must not exceed the configured upper bound.

$$
s.t. \quad \text{assigned\_length}_{i} \leq \text{upperBound}_{i}, \; \forall i \in P^{bound}
$$

### 3. Over-Length Upper Bound Constraint

**[CN]**：超长上限约束
**Description**: The over-length slack variable must not exceed the configured over-length upper bound.

$$
s.t. \quad \text{over\_length}_{i} \leq \text{overLengthUpperBound}_{i}, \; \forall i \in P^{dyn}
$$

### 4. Assigned-Over Length Link Constraint

**[CN]**：卷长-超长关联约束
**Description**: The assigned length minus over-length slack must not exceed the maximum overproduce length.

$$
s.t. \quad \text{assigned\_length}_{i} - \text{over\_length}_{i} \leq \text{maxOverProduceLength}_{i}, \; \forall i \in P^{dyn}
$$

---

## 10. Objective Function

**Description**: Minimize total length penalty and over-length penalty.

$$
\min \sum_{i \in P^{dyn}} (\text{totalLengthPenalty} \times \text{assigned\_length}_{i} + \text{overLengthPenalty}_{i} \times \text{over\_length}_{i})
$$

---

## 11. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Length Derivation Function | - | Section 6 | Derive coil length from demand quantity and product properties |

---

## 12. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Assigned Length | $\text{assigned\_length}$ | Decision coil length for dynamic-length products |
| Over Length | $\text{over\_length}$ | Portion exceeding maximum overproduce length |
| Over-Length Record | - | Entity recording product over-length quantity |
| Length Assignment | - | Assigned coil length result for a single product |
| Length Derivation | - | Function deriving coil length from demand quantity |

---

## 13. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Use dimensionless normalized coefficients | Quantities with units | Unit conversion responsibility delegated to caller | - |
| Separate assignedLength and overLength | Single combined variable | Over-length can be independently penalized and constrained | - |
| Inject derivation logic via LengthDerivation functional interface | Hardcoded derivation logic | Supports downstream project customization per business unit | - |

---

## 14. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial version | Dynamic coil length assignment foundation |
