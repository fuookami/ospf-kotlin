# Cargo Domain Model

:us: English | :cn: [简体中文](domain-model_ch.md)

[toc]

## 1. Overview

Manages cargo domain operations in the flight recovery scheduling system, including cargo capacity tracking and disruption handling. Currently a placeholder context预留 for future cargo routing and capacity optimization.

### 1. Dependent Contexts

1. **task** (Flight Task)
2. **rule** (Rule)

---

## 2. Concepts / Entities

### 1. Cargo Capacity

The cargo transport capacity of an aircraft, measured by weight or volume.

**$\text{capacity}_{a}$** : Cargo capacity of aircraft $a$.
**$\text{cargo\_task}_{i}$** : Whether flight task $i$ is a cargo flight.

---

## 3. Variables

> This context is currently a placeholder; no decision variables are defined yet.

---

## 4. Predicates

### 1. Capacity Type

**isCargo** : Aircraft $a$ has cargo capacity type (`AircraftCapacity.Cargo`).

---

## 5. Sets

### 1. Cargo Flights

**$I^{C}$** : Set of all cargo-type flight tasks, $I^{C} = \{ i \in I \mid \text{isCargo}(i) \}$.

---

## 6. Intermediate Values

> This context is currently a placeholder; no intermediate values are defined yet.

---

## 7. Assertions

> This context is currently a placeholder; no assertions are defined yet.

---

## 8. Constraints

### 1. Cargo Capacity Constraint

**[CN]**: 货物容量约束
**Description**: The total load of each cargo flight must not exceed the aircraft's cargo capacity.

$$
s.t. \quad \text{cargo\_load}_{i} \leq \text{capacity}_{a(i)}, \; \forall i \in I^{C}
$$

---

## 9. Objective Function

> This context is currently a placeholder; no独立 objective function is defined yet.

---

## 10. Algorithm References

> No algorithm references in this context.

---

## 11. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Cargo Capacity | capacity | The cargo transport capability of an aircraft |
| Cargo Flight | $I^{C}$ | A flight task that transports cargo |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Placeholder cargo context | Direct modeling in task | Follows bounded context separation principle,便于 future extension | - |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1 | Placeholder implementation | Reserve for cargo domain extension |
