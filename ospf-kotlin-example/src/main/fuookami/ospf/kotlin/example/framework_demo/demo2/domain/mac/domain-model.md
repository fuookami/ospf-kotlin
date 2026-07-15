# Mean Aerodynamic Chord (MAC) Domain Model

[toc]

## 1. Overview

Computes Mean Aerodynamic Chord (MAC) percentage, longitudinal/lateral torque, CLIM, and index for each flight phase from aircraft and stowage data.

### 1. Dependent Contexts

1. Aircraft
2. Stowage

---

## 2. Concepts / Entities

### 1. Torque

Computes longitudinal torque, lateral torque, CLIM, and index per flight phase from load, fuel, fuselage, and formula data.

**$longitudinalTorque_{phase}$** : Longitudinal torque per flight phase.
**$lateralTorque$** : Lateral torque (wide-body only).
**$clim$** : CLIM (Center of Gravity Index Moment).
**$index_{phase}$** : Index per flight phase.

### 2. MAC

Computes MAC percentage as a linear intermediate symbol from torque index and total weight.

**$mac$** : MAC percentage, linear intermediate symbol.

### 3. Horizontal Stabilizer

Horizontal stabilizer position and limits for balance computation.

**$key$** : Horizontal stabilizer identifier.
**$points$** : Horizontal stabilizer data points.
**$limit$** : Horizontal stabilizer limit.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Intermediate Values

### 1. Longitudinal Torque

**Description**: Longitudinal torque per flight phase, composed of load torque, fuel torque, fuselage moment, and liferaft moment.

$$
longitudinalTorque_{phase} = \sum_{j \in J} loadLongitudinalTorque_j + fuelWeight_{phase} \cdot fuelArm_{phase} + dow \cdot balancedArm + liferaftWeight \cdot liferaftArm
$$

### 2. MAC Percentage

**Description**: Mean Aerodynamic Chord percentage, computed from torque index and total weight.

$$
mac = \frac{index_{TakeOff}}{tow} \cdot 100\%
$$

---

## 5. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 扭矩 | Torque | Torque | Force times arm distance |
| 平均气动弦 | MAC | Mean Aerodynamic Chord | Wing mean aerodynamic chord length |
| 水平安定面 | HorizontalStabilizer | Horizontal Stabilizer | Tail horizontal stabilizer |
| CLIM | CLIM | Center of Gravity Index Moment | Center of gravity index moment |

---

## 6. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| MAC computation formula | Linearized vs Non-linear | Linearization facilitates optimization model solving | 2024 |
