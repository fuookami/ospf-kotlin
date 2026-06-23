# Redundancy Domain Model

[toc]

## 1. Overview

Manages redundancy and experimental longitudinal balance constraints for weight distribution analysis and safety margins.

### 1. Dependent Contexts

1. Aircraft
2. Stowage

---

## 2. Concepts / Entities

### 1. Redundancy

Redundancy model for weight distribution safety margins.

**$redundancy$** : Redundancy value.

### 2. Experimental Longitudinal Balance

Experimental longitudinal balance model based on redundancy computations.

**$experimentalBalance$** : Experimental longitudinal balance value.
**$redundancy$** : Dependent redundancy value.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Constraints

### 1. Redundancy Limit

**[CN]**: 冗余限制
**Description**: Redundancy must be within acceptable bounds.

$$
s.t. \quad minRedundancy \leq redundancy \leq maxRedundancy
$$

### 2. Experimental Longitudinal Balance Limit

**[CN]**: 实验纵向平衡限制
**Description**: Experimental longitudinal balance must be within bounds.

$$
s.t. \quad minBalance \leq experimentalBalance \leq maxBalance
$$

---

## 5. Objective Function

This context does not define an objective function; it only provides constraints.

---

## 6. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 冗余 | Redundancy | Redundancy | Safety margin for weight distribution |
| 实验纵向平衡 | ExperimentalLongitudinalBalance | Experimental Longitudinal Balance | Longitudinal balance based on redundancy |

---

## 7. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Redundancy modeling | Independent vs Coupled | Coupling with longitudinal balance matches reality | 2024 |
