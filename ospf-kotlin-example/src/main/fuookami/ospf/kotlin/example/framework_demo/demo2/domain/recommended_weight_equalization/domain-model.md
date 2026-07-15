# Recommended Weight Equalization Domain Model

[toc]

## 1. Overview

Manages recommended weight equalization — ensuring cargo weight is distributed evenly across positions according to priority appointments.

### 1. Dependent Contexts

1. Aircraft
2. Stowage

---

## 2. Concepts / Entities

### 1. Priority Appointment

Priority-based appointment of items to positions with weight equalization.

**$appointment$** : Item-to-position appointment mapping.
**$priority$** : Item priority.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Constraints

### 1. Item Order Limit

**[CN]**: 货物顺序限制
**Description**: Items must be loaded in priority order.

$$
s.t. \quad priority_i < priority_j \rightarrow order_i \leq order_j, \; \forall i, j \in Items
$$

### 2. Priority Appointment Limit

**[CN]**: 优先级预约限制
**Description**: Priority appointments must be respected.

$$
s.t. \quad x_{ij} = 1, \; \forall (i, j) \in PriorityAppointment
$$

### 3. Recommended Weight Equalization Limit

**[CN]**: 推荐重量均衡限制
**Description**: Load weight should equalize across positions.

$$
s.t. \quad |loadWeight_j - avgWeight| \leq tolerance, \; \forall j \in J
$$

---

## 5. Objective Function

Minimize weight deviation from recommended values.

$$
\min \sum_{j \in J} |loadWeight_j - recommendedWeight_j|
$$

---

## 6. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 优先级预约 | PriorityAppointment | Priority Appointment | Priority-based item-to-position appointment |
| 重量均衡 | WeightEqualization | Weight Equalization | Even distribution of cargo weight |

---

## 7. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Equalization strategy | Absolute vs Relative | Relative equalization is more flexible | 2024 |
