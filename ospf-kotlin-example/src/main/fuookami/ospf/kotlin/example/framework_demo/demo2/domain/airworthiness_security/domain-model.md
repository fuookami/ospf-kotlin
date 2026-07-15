# Airworthiness Security Domain Model

[toc]

## 1. Overview

Enforces airworthiness and safety constraints including linear/surface density limits, zone load weight limits, cumulative load weight limits, CLIM limits, envelope constraints, and payload limits.

### 1. Dependent Contexts

1. Aircraft
2. Stowage
3. Mean Aerodynamic Chord (MAC)

---

## 2. Concepts / Entities

### 1. Linear Density

Linear weight density per fuselage zone with upper/lower limits.

**$limitZones$** : List of limit zones.
**$density_{zone}$** : Zone linear density value.

### 2. Surface Density

Surface weight density per zone with upper/lower limits.

**$limitZones$** : List of limit zones.
**$density_{zone}$** : Zone surface density value.

### 3. Max Zone Load Weight

Maximum allowable load weight per fuselage zone.

**$maxWeight_{zone}$** : Maximum zone load weight.

### 4. Max Cumulative Load Weight

Maximum cumulative load weight from nose/tail.

**$maxWeight_{fromNose}$** : Maximum cumulative weight from nose.
**$maxWeight_{fromTail}$** : Maximum cumulative weight from tail.

### 5. Max Unsymmetrical Linear Density

Maximum allowable unsymmetrical linear density for wide-body aircraft.

**$maxDensity$** : Maximum unsymmetrical linear density.

### 6. Max CLIM

Maximum CLIM limits for wide-body aircraft.

**$points$** : CLIM limit data points.

### 7. Min Low Payload

Minimum payload required in the lower deck.

**$points$** : Minimum payload data points.

### 8. Envelope

Weight-CG envelope constraints per flight phase.

**$envelopes_{phase}$** : Envelope per flight phase.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Intermediate Values

### 1. Linear Density

**Description**: Linear weight density per fuselage zone.

$$
linearDensity_{zone} = \frac{\sum_{j \in zone} loadWeight_j}{zoneLength}
$$

### 2. Surface Density

**Description**: Surface weight density per zone.

$$
surfaceDensity_{zone} = \frac{\sum_{j \in zone} loadWeight_j}{zoneArea}
$$

---

## 5. Constraints

### 1. Linear Density Limit

**[CN]**: 线密度限制
**Description**: Linear density per zone must be within limits.

$$
s.t. \quad linearDensityMin_{zone} \leq linearDensity_{zone} \leq linearDensityMax_{zone}, \; \forall zone \in Zones
$$

### 2. Surface Density Limit

**[CN]**: 面密度限制
**Description**: Surface density per zone must be within limits.

$$
s.t. \quad surfaceDensityMin_{zone} \leq surfaceDensity_{zone} \leq surfaceDensityMax_{zone}, \; \forall zone \in Zones
$$

### 3. Zone Load Weight Limit

**[CN]**: 区域载荷重量限制
**Description**: Zone load weight must not exceed maximum.

$$
s.t. \quad \sum_{j \in zone} loadWeight_j \leq maxZoneLoadWeight_{zone}, \; \forall zone \in Zones
$$

### 4. Cumulative Load Weight Limit

**[CN]**: 累积载荷重量限制
**Description**: Cumulative load weight from nose/tail must not exceed maximum.

$$
s.t. \quad \sum_{j \leq k} loadWeight_j \leq maxCumulativeWeight_k, \; \forall k \in Positions
$$

### 5. Envelope Limit

**[CN]**: 包络线限制
**Description**: Weight-CG combination must be within the envelope per flight phase.

$$
s.t. \quad (weight_{phase}, mac) \in envelope_{phase}, \; \forall phase \in FlightPhases
$$

### 6. Payload Limit

**[CN]**: 载荷限制
**Description**: Payload must be within planned and maximum bounds.

$$
s.t. \quad plannedPayload \leq payload \leq maxPayload
$$

### 7. Total Weight Limit

**[CN]**: 总重量限制
**Description**: Total weight per flight phase must not exceed maximum.

$$
s.t. \quad totalWeight_{phase} \leq maxTotalWeight_{phase}, \; \forall phase \in FlightPhases
$$

---

## 6. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 线密度 | LinearDensity | Linear Density | Weight per unit length |
| 面密度 | SurfaceDensity | Surface Density | Weight per unit area |
| 包络线 | Envelope | Envelope | Weight-CG feasible region |
| CLIM | CLIM | Center of Gravity Index Moment | Center of gravity index moment |
| 区域载荷 | ZoneLoadWeight | Zone Load Weight | Load weight per fuselage zone |
| 累积载荷 | CumulativeLoadWeight | Cumulative Load Weight | Cumulative load from endpoint |

---

## 7. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Envelope modeling | Linearized vs Piecewise linear | Piecewise linear is more accurate | 2024 |
| Benders decomposition | Airworthiness in sub problem | Airworthiness constraints strongly coupled with stowage | 2024 |
