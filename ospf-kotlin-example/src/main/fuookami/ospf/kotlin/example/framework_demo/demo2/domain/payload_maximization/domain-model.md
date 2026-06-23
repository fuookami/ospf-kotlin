# Payload Maximization Domain Model

[toc]

## 1. Overview

Maximizes the total payload (cargo weight) loaded onto the aircraft within all safety and structural constraints.

### 1. Dependent Contexts

1. Aircraft
2. Stowage

---

## 2. Concepts / Entities

### 1. Payload

Total cargo weight to be maximized.

**$payload$** : Total payload amount.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Constraints

### 1. Max Payload Limit

**[CN]**: 最大载荷限制
**Description**: Payload must not exceed aircraft maximum payload capacity.

$$
s.t. \quad payload \leq maxPayload
$$

---

## 5. Objective Function

Maximize total payload.

$$
\max \sum_{i \in I} \sum_{j \in J} weight_i \cdot x_{ij}
$$

---

## 6. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 载荷 | Payload | Payload | Total cargo weight on aircraft |
| 最大载荷 | MaxPayload | Max Payload | Maximum aircraft payload capacity |

---

## 7. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Objective function | Payload maximization vs Cost minimization | Payload maximization is the primary business goal | 2024 |
