# MAC Optimization Domain Model

[toc]

## 1. Overview

Manages MAC optimization including longitudinal balance (MAC range constraints) and lateral balance constraints for aircraft weight distribution.

### 1. Dependent Contexts

1. Aircraft
2. Stowage
3. Mean Aerodynamic Chord (MAC)

---

## 2. Concepts / Entities

### 1. MAC Range

Defines allowable MAC percentage range based on total weight.

**$minMAC_{weight}$** : Minimum MAC percentage at given total weight.
**$maxMAC_{weight}$** : Maximum MAC percentage at given total weight.

### 2. Longitudinal Balance

Longitudinal balance constraints ensuring MAC is within allowable range per flight phase.

**$macRange$** : MAC range.
**$torque$** : Torque data.

### 3. Lateral Balance

Lateral balance constraints for wide-body aircraft ensuring symmetrical loading.

**$torque$** : Lateral torque data.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Constraints

### 1. Longitudinal Balance Limit

**[CN]**: 纵向平衡限制
**Description**: MAC percentage must be within allowable range per flight phase.

$$
s.t. \quad minMAC_{weight} \leq mac \leq maxMAC_{weight}, \; \forall phase \in FlightPhases
$$

### 2. Lateral Balance Limit

**[CN]**: 横向平衡限制
**Description**: Lateral torque must be within allowable range (wide-body only).

$$
s.t. \quad |lateralTorque| \leq maxLateralTorque
$$

### 3. Horizontal Stabilizer Limit

**[CN]**: 水平安定面限制
**Description**: Horizontal stabilizer position must match MAC.

$$
s.t. \quad stabilizerPosition = f(mac), \; \forall phase \in FlightPhases
$$

---

## 5. Objective Function

Minimize MAC deviation from target range.

$$
\min |mac - macTarget|
$$

---

## 6. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| MAC 范围 | MACRange | MAC Range | Allowable MAC percentage range |
| 纵向平衡 | LongitudinalBalance | Longitudinal Balance | Fore-aft weight balance |
| 横向平衡 | LateralBalance | Lateral Balance | Left-right weight balance |
| 水平安定面 | HorizontalStabilizer | Horizontal Stabilizer | Tail horizontal stabilizer |

---

## 7. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| MAC range modeling | Linear vs Piecewise linear | Piecewise linear is more accurate | 2024 |
