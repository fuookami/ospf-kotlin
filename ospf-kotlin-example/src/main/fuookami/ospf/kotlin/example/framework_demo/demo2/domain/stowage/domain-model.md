# Stowage Domain Model

[toc]

## 1. Overview

Manages cargo stowage assignment decisions — which items go to which positions — including load weight computation, payload calculation, total weight, and max load weight.

### 1. Dependent Contexts

1. Aircraft

---

## 2. Concepts / Entities

### 1. Item

A cargo item with destination, weight, ULD, location tags, cargo type, priority, and status.

**$id_{i}$** : Item unique identifier.
**$dest_{i}$** : Destination (IATA code).
**$weight_{i}$** : Item weight, unit kg.
**$uld_{i}$** : Associated ULD (optional).
**$location_{i}$** : Location tags (Main/Low/Bulk/Head/Tail).
**$cargo_{i}$** : Cargo type and priority.
**$status_{i}$** : Status (Loaded/Preassigned/Optional/Reserved/AdjustmentNeeded).
**$order_{i}$** : Order information (hardstand time, reweigh time, car-board info).

### 2. Position

Stowage position with max load amount (MLA), predicate load weight (PLW), recommended load weight, and status.

**$spaceName_{j}$** : Space name.
**$mla_{j}$** : Max Load Amount.
**$plw_{j}$** : Predicate Load Weight, its definition is further specified in *Stowage*.
**$mlw_{j}$** : Max Load Weight, its definition is further specified in *Airworthiness Security*.
**$coordinate_{j}$** : Coordinate (longitudinal arm, lateral arm).
**$location_{j}$** : Set of location tags.

### 3. Flight

Flight information.

**$flightNo$** : Flight number.
**$departure$** : Departure airport (IATA code).
**$arrival$** : Arrival airport (IATA code).

### 4. Appointment

Pre-assigned item-to-position appointments.

**$appointment$** : Item-to-position pre-assignment mapping.

### 5. Ballast

Ballast weight for balance correction.

**$minBallastWeight$** : Minimum ballast weight.

---

## 3. Variables

### 1. Decision Variables

**$x_{ij}$** : Whether item $i$ is assigned to position $j$, binary variable, domain is $\{0, 1\}$, $\forall i \in Items$, $\forall j \in Positions$.

**$y_{j}$** : Predicate load weight at position $j$, continuous variable (kg), domain is $[0, MLW_j]$, $\forall j \in Positions$.

**$z_{j}$** : Recommended load weight at position $j$, integer variable (kg), domain is $[0, \lfloor MLW_j \rfloor]$, $\forall j \in Positions$.

### 2. Auxiliary Variables

**$u_{ij}$** : Adjustment variable for item $i$ at position $j$, binary variable, domain is $\{0, 1\}$, $\forall i \in Items$, $\forall j \in Positions$.

---

## 4. Predicates

### 1. Item Status Predicates

**stowageNeeded(item)** : Item requires position assignment (status is Preassigned or Optional).
**adjustmentNeeded(item)** : Item requires position adjustment (status is AdjustmentNeeded).
**loaded(item)** : Item is loaded (status is Loaded or AdjustmentNeeded).

### 2. Position Status Predicates

**stowageNeeded(position)** : Position requires item assignment.
**available(position)** : Position is available for loading.
**predicateWeightNeeded(position)** : Position requires predicate weight variable.
**recommendedWeightNeeded(position)** : Position requires recommended weight variable.

---

## 5. Sets

### 1. Items

**$I$** : Set of all cargo items.

**$I^{pre}$** : Subset of items satisfying predicate stowageNeeded, items requiring position assignment.
**$I^{adj}$** : Subset of items satisfying predicate adjustmentNeeded, items requiring position adjustment.
**$I^{opt}$** : Subset of items with Optional status, optionally loaded items.

### 2. Positions

**$J$** : Set of all stowage positions.

**$J^{avl}$** : Subset of positions satisfying predicate available, positions available for loading.
**$J^{pw}$** : Subset of positions satisfying predicate predicateWeightNeeded, positions requiring predicate weight.
**$J^{rw}$** : Subset of positions satisfying predicate recommendedWeightNeeded, positions requiring recommended weight.

### 3. Item-Position Pairs

**$IJ^{feas}$** : Set of feasible item-position assignment pairs, i.e., $(i, j)$ pairs satisfying stowageNeeded(item, position).

---

## 6. Intermediate Values

### 1. Load Amount

**Description**: Number of items loaded at each position.

$$
loadAmount_j = \sum_{i \in I} stowage_{ij}, \; \forall j \in J
$$

### 2. Actual Load Weight

**Description**: Actual load weight at each position.

$$
actualLoadWeight_j = \sum_{i \in I} weight_i \cdot stowage_{ij}, \; \forall j \in J
$$

### 3. Estimate Load Weight

**Description**: Estimated load weight including predicate and recommended weights.

$$
estimateLoadWeight_j = \sum_{i \in I} weight_i \cdot stowage_{ij} + y_j + z_j, \; \forall j \in J
$$

---

## 7. Constraints

### 1. Item Assignment Limit

**[CN]**: 货物分配限制
**Description**: Each item requiring stowage must be assigned to exactly one position.

$$
s.t. \quad \sum_{j \in J} x_{ij} = 1, \; \forall i \in I^{pre}
$$

### 2. Load Amount Limit

**[CN]**: 装载数量限制
**Description**: Load amount per position must not exceed the maximum load amount (MLA).

$$
s.t. \quad \sum_{i \in I} stowage_{ij} \leq mla_j, \; \forall j \in J
$$

### 3. Load Weight Limit

**[CN]**: 装载重量限制
**Description**: Load weight per position must not exceed the maximum load weight (MLW).

$$
s.t. \quad actualLoadWeight_j \leq mlw_j, \; \forall j \in J
$$

### 4. Appointment Limit

**[CN]**: 预约限制
**Description**: Pre-assigned item-to-position appointments must be respected.

$$
s.t. \quad x_{ij} = 1, \; \forall (i, j) \in Appointment
$$

---

## 8. Objective Function

This context does not define an independent objective function; it only provides constraints.

---

## 9. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 货物项 | Item | Item | Cargo unit to be loaded |
| 装载位置 | Position | Position | Cargo loading position on aircraft |
| 装载分配 | Stowage | Stowage | Item-to-position assignment decision |
| 装载量 | Load | Load | Load weight and amount at position |
| 载荷 | Payload | Payload | Total cargo weight on aircraft |
| 总重量 | TotalWeight | Total Weight | Aircraft total weight per flight phase |
| 最大装载重量 | MaxLoadWeight | Max Load Weight | Maximum allowable load weight at position |
| 压舱物 | Ballast | Ballast | Ballast weight for balance |
| 谓词装载重量 | PLW | Predicate Load Weight | Predicted load weight |
| 最大装载数量 | MLA | Max Load Amount | Maximum load amount at position |

---

## 10. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Stowage Mode Selection | FullLoad / Predistribution / WeightRecommendation | Different modes for different business scenarios | 2024 |
| Benders Decomposition | Master/Sub problem separation | Airworthiness constraints in sub problem, others in master | 2024 |
