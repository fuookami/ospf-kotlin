# Express Effectiveness Domain Model

[toc]

## 1. Overview

Manages express effectiveness constraints that optimize item priority ordering — ensuring high-priority cargo is loaded preferentially.

### 1. Dependent Contexts

1. Aircraft
2. Stowage

---

## 2. Concepts / Entities

### 1. Absolute Order

Defines absolute priority ordering for predistribution mode.

**$order_{i}$** : Absolute priority order of item $i$.

### 2. Relative Order

Defines relative priority ordering for full-load mode.

**$relativeOrder_{ij}$** : Priority order of item $i$ relative to item $j$.

### 3. Must-Ship Items

Items that must be shipped regardless of priority.

**$mustShipIndices$** : Index list of must-ship items.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Constraints

### 1. Must-Ship Limit

**[CN]**: 必须发运限制
**Description**: Must-ship items must be loaded.

$$
s.t. \quad \sum_{j \in J} x_{ij} = 1, \; \forall i \in MustShip
$$

### 2. Item Priority Limit

**[CN]**: 货物优先级限制
**Description**: Higher priority items should be loaded before lower priority items.

$$
s.t. \quad priority_i < priority_j \rightarrow loaded_i \geq loaded_j, \; \forall i, j \in Items
$$

---

## 5. Objective Function

Minimize priority violation cost.

$$
\min \sum_{i \in I} priorityViolationCost_i
$$

---

## 6. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 绝对排序 | AbsoluteOrder | Absolute Order | Priority ordering for predistribution mode |
| 相对排序 | RelativeOrder | Relative Order | Priority ordering for full-load mode |
| 必须发运项 | MustShip | Must-Ship Items | Items that must be shipped |

---

## 7. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Ordering mode | Absolute vs Relative | Different stowage modes use different ordering strategies | 2024 |
