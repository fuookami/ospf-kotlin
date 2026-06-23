# Aircraft Domain Model

[toc]

## 1. Overview

Manages aircraft configuration data including model classification, fuselage parameters, fuel constants, deck layout, cargo positions, and neighbour relationships. Provides base data for all downstream optimization contexts.

### 1. Dependent Contexts

No upstream dependencies.

---

## 2. Concepts / Entities

### 1. Aircraft Model

Aircraft type classification (B737/B757/B767/B747) with physical unit definitions.

**$type$** : Aircraft type enumeration (B737, B757, B767, B747).
**$model$** : Aircraft model string.
**$minorModel$** : Aircraft minor model.

### 2. Fuselage

Aircraft fuselage properties including DOW, balanced arm, DOI, and liferaft.

**$dow$** : Dry Operating Weight, unit kg.
**$balancedArm$** : Balanced arm, unit inch.
**$doi$** : Dry Operating Index.
**$liferaft$** : Liferaft information (optional).

### 3. Deck

Physical deck on the aircraft with doors, cargo positions, and door proximity mappings.

**$location$** : Deck location (Main / LowForward / LowAft).
**$doors$** : List of hatch doors on the deck.
**$positions$** : List of cargo positions on the deck.
**$doorUbieties$** : Door-to-position proximity mapping.

### 4. Position

Cargo position with coordinates (longitudinal/lateral arm), shape, location tags, and loading order.

**$id$** : Position unique identifier.
**$spaceName$** : Space name (e.g., "1L", "2R").
**$sizeCode$** : Size code.
**$coordinate$** : Coordinate (longitudinal arm, lateral arm).
**$shape$** : Physical dimensions (length, width, volume, area).
**$location$** : Set of location tags (Main/Low/Bulk/Head/Tail).
**$linearLoadingOrder$** : Linear loading order.

### 5. Fuel

Fuel constants per flight phase.

**$weight$** : Fuel weight.
**$index$** : Fuel index.

### 6. ULD

Unit Load Device.

**$code$** : ULD code.
**$sizeCode$** : Size code.

### 7. Neighbour

Adjacency relationships between positions for constraint generation.

**$type$** : Neighbour type.
**$positions$** : Adjacent position pairs.

### 8. Loading Order

Defines the order in which positions should be loaded.

**$order$** : Order value.

---

## 3. Variables

### 1. Decision Variables

This context does not define decision variables; it only provides configuration data.

---

## 4. Predicates

### 1. Aircraft Type Predicates

**narrowBody(type)** : Narrow-body aircraft (B737, B757).
**wideBody(type)** : Wide-body aircraft (B767, B747).
**ballastNeeded(type)** : Ballast required (B757, B767).
**mainDeckDoorEmptyPrefer(type)** : Main deck door empty preference (B757, B767).

---

## 5. Sets

### 1. Decks

**$D$** : Set of all decks.

**$D^{main}$** : Main deck subset.
**$D^{low}$** : Lower deck subset.

### 2. Positions

**$J$** : Set of all cargo positions.

**$J^{main}$** : Main deck position subset.
**$J^{low}$** : Lower deck position subset.
**$J^{bulk}$** : Bulk position subset.

---

## 6. Constraints

This context does not define constraints.

---

## 7. Objective Function

This context does not define an objective function.

---

## 8. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 飞机型号 | AircraftModel | Aircraft Model | Aircraft type classification with physical unit definitions |
| 机身 | Fuselage | Fuselage | Aircraft fuselage properties |
| 甲板 | Deck | Deck | Physical deck on the aircraft |
| 货物位置 | Position | Position | Cargo loading position on the aircraft |
| 燃油 | Fuel | Fuel | Fuel constants per flight phase |
| 集装器 | ULD | Unit Load Device | Standardized cargo container |
| 邻接关系 | Neighbour | Neighbour | Adjacency relationship between positions |
| 装载顺序 | LoadingOrder | Loading Order | Loading order of positions |

---

## 9. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Physical unit system | Imperial (inch/ft/lb) vs Metric | Aviation industry standard uses imperial | 2024 |
| Location tag system | Enum vs Bitmap | Enum is more intuitive, facilitates predicate definitions | 2024 |
