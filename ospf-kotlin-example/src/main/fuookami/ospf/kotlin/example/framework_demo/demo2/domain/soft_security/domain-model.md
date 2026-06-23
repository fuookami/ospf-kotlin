# Soft Security Domain Model

[toc]

## 1. Overview

Manages soft safety constraints including empty loading division, main deck door empty preference, and ballast weight advice — constraints that improve safety but can be relaxed if needed.

### 1. Dependent Contexts

1. Aircraft
2. Stowage

---

## 2. Concepts / Entities

### 1. Divide Empty Loading

Ensures empty positions are divided rather than clustered for structural safety.

**$positions$** : Position list.
**$load$** : Load data.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Constraints

### 1. Empty Hated Limit

**[CN]**: 空载厌恶限制
**Description**: Penalty for empty positions (soft preference to fill positions).

$$
s.t. \quad \sum_{j \in J} empty_j \leq maxEmpty \quad (\text{soft constraint})
$$

### 2. Main Deck Door Empty Limit

**[CN]**: 主甲板舱门空载限制
**Description**: Main deck door positions should preferentially be empty (B757/B767).

$$
s.t. \quad empty_j = 1, \; \forall j \in MainDeckDoorPositions \quad (\text{soft constraint})
$$

### 3. Divide Empty Loading Limit

**[CN]**: 空载分离限制
**Description**: Empty positions should be distributed across the aircraft.

$$
s.t. \quad \text{empty positions should be distributed} \quad (\text{soft constraint})
$$

### 4. Advice Ballast Weight Limit

**[CN]**: 建议压舱物重量限制
**Description**: Ballast weight should meet advisory minimum.

$$
s.t. \quad ballastWeight \geq adviceBallastWeight \quad (\text{soft constraint})
$$

---

## 5. Objective Function

Minimize soft security violation penalties.

$$
\min \sum penalties
$$

---

## 6. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 空载分离 | DivideEmptyLoading | Divide Empty Loading | Distributed empty positions |
| 空载厌恶 | EmptyHated | Empty Hated | Soft penalty for empty positions |
| 压舱物建议 | AdviceBallastWeight | Advice Ballast Weight | Advisory ballast weight value |

---

## 7. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Constraint type | Hard vs Soft | Soft constraints allow relaxation when needed | 2024 |
