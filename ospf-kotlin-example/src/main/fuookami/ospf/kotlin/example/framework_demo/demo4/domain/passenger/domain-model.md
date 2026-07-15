# Passenger Domain Model

:us: English | :cn: [简体中文](domain-model_ch.md)

[toc]

## 1. Overview

Manages passenger cancellations, class changes, flight changes, and amount tracking in the flight recovery scheduling system, registering passenger-related constraints and objectives into the column generation model.

### 1. Dependent Contexts

1. **task** (Flight Task)
2. **bunch_compilation** (Bunch Compilation)

---

## 2. Concepts / Entities

### 1. Passenger

A passenger with an amount and a multi-leg flight list, each leg assigned a passenger class.

**$\text{id}_{p}$** : Unique identifier of passenger $p$.
**$\text{amount}_{p}$** : Amount of passenger $p$ (greater than 1 for group passengers).
**$\text{flights}_{p}$** : Leg list of passenger $p$, each entry is a (flight task, class) pair.
**$\text{route}_{p}$** : Airport route of passenger $p$.

### 2. Flight Passenger (FlightPassenger)

An association linking a passenger to a specific flight with an optional previous leg.

**$\text{flight}_{fp}$** : Associated flight task.
**$\text{passenger}_{fp}$** : Associated passenger.
**$\text{prev}_{fp}$** : Previous leg's flight passenger association (optional).
**$\text{cls}_{fp}$** : Passenger's class on this flight.
**$\text{amount}_{fp}$** : Passenger amount.

### 3. Passenger Cancel (PassengerCancel)

Tracks passenger cancellation decision variables in the column generation formulation.

**$\text{passengerCancel}_{fp}$** : Cancellation variable for flight passenger $fp$.

### 4. Passenger Change (PassengerChange)

Tracks passenger class change and flight change decision variables in the column generation formulation.

**$\text{passengerClassChange}_{fp,cls}$** : Variable for flight passenger $fp$ changing to class $cls$.
**$\text{passengerFlightChange}_{fp,f',cls}$** : Variable for flight passenger $fp$ changing to flight $f'$ and class $cls$.

### 5. Passenger Amount (PassengerAmount)

Computes passenger amount expressions per flight and class, accounting for cancellations and changes.

**$\text{passengerAmount}_{f,cls}$** : Passenger amount expression for flight $f$ at class $cls$.

---

## 3. Variables

### 1. Decision Variables

**$c_{fp}$** : Cancellation amount for flight passenger $fp$, dimensionless, domain $[0, \text{amount}_{fp}]$, integer, represents the number of cancelled passengers, $\forall fp \in FP$.

**$s_{fp,cls}$** : Class change amount for flight passenger $fp$ to class $cls$, dimensionless, domain $[0, \text{amount}_{fp}]$, integer, $\forall fp \in FP, \forall cls \in CLS \setminus \{fp.cls\}$.

**$r_{fp,f',cls}$** : Flight change amount for flight passenger $fp$ to flight $f'$ and class $cls$, dimensionless, domain $[0, \text{amount}_{fp}]$, integer, $\forall fp \in FP, \forall f' \in \text{toFlights}_{fp.flight}, \forall cls \in CLS$.

### 2. Auxiliary Variables

> No additional auxiliary variables.

---

## 4. Predicates

### 1. Passenger Status

**isCancelled** : Flight passenger $fp$ is cancelled ($c_{fp} > 0$).
**isClassChanged** : Flight passenger $fp$ has a class change.
**isFlightChanged** : Flight passenger $fp$ has a flight change.
**isTransfer** : Passenger $p$ is a transfer passenger (route contains more than 2 airports).

---

## 5. Sets

### 1. Flight Passengers

**$FP$** : Universal set of all flight passenger associations.

**$FP_{f}$** : Subset of flight passengers on flight $f$, $\forall f \in F$.
**$FP_{p}$** : Subset of flight passengers for passenger $p$, $\forall p \in P$.

### 2. Flights

**$F$** : Universal set of all flight tasks.

**$F_{fp}$** : Subset of alternative flights for flight passenger $fp$ (same origin and destination).

### 3. Classes

**$CLS$** : Universal set of all passenger classes (PassengerClass enum).

---

## 6. Intermediate Values

### 1. Passenger Amount Expression

**Description**: Net passenger amount for flight $f$ at class $cls$, accounting for cancellations, class changes, and flight changes.

$$
\text{passengerAmount}_{f,cls} = \sum_{fp \in FP_{f}} \left( \mathbb{1}[fp.cls = cls] \cdot \text{amount}_{fp} - c_{fp} - \mathbb{1}[fp.cls = cls] \cdot \sum_{cls' \in CLS} s_{fp,cls'} - \mathbb{1}[fp.cls = cls] \cdot \sum_{f', cls'} r_{fp,f',cls'} + \sum_{cls'} s_{fp,cls} + \sum_{fp' \in FP : f \in F_{fp'}} r_{fp',f,cls} \right)
$$

---

## 7. Assertions

### 1. Passenger Route Continuity

**Description**: In a passenger's leg list, the arrival airport of a consecutive leg must match the departure airport of the next leg.

$$
\forall p \in P, \forall k \in [1, |\text{flights}_{p}|) \; (\text{flights}_{p}[k-1].arr = \text{flights}_{p}[k].dep)
$$

### 2. Flight Type Consistency

**Description**: All legs of a passenger must be flight types.

$$
\forall p \in P, \forall (f, cls) \in \text{flights}_{p} \; (f.\text{isFlight})
$$

---

## 8. Constraints

### 1. Passenger Cancel Minimization

**[CN]**: 乘客取消最小化
**Description**: Minimize the total number of cancelled passengers (objective function term).

$$
\min \sum_{fp \in FP} w_{fp} \cdot c_{fp}
$$

### 2. Passenger Class Change Minimization

**[CN]**: 乘客舱位变更最小化
**Description**: Minimize the total number of class-changed passengers (objective function term).

$$
\min \sum_{fp \in FP} \sum_{cls \in CLS \setminus \{fp.cls\}} w_{fp} \cdot s_{fp,cls}
$$

### 3. Passenger Flight Change Minimization

**[CN]**: 乘客航班变更最小化
**Description**: Minimize the total number of flight-changed passengers (objective function term).

$$
\min \sum_{fp \in FP} \sum_{f' \in F_{fp}} \sum_{cls \in CLS} w_{fp} \cdot r_{fp,f',cls}
$$

### 4. Passenger Flight Capacity Constraint

**[CN]**: 航班乘客容量约束
**Description**: The passenger amount per flight per class must not exceed available capacity.

$$
s.t. \quad \text{passengerAmount}_{f,cls} \leq \text{capacity}_{f,cls}, \; \forall f \in F, \forall cls \in CLS
$$

### 5. Passenger Route Cancel Constraint

**[CN]**: 路线取消约束
**Description**: If any leg of a passenger is cancelled, all legs of the entire route are cancelled.

$$
s.t. \quad c_{fp} = c_{fp'}, \; \forall p \in P, \forall fp, fp' \in FP_{p}
$$

---

## 9. Objective Function

**Description**: Minimize the weighted sum of passenger cancellations, class changes, and flight changes.

$$
\min \sum_{fp \in FP} \left( w^{cancel}_{fp} \cdot c_{fp} + \sum_{cls} w^{class}_{fp} \cdot s_{fp,cls} + \sum_{f', cls} w^{flight}_{fp} \cdot r_{fp,f',cls} \right)
$$

---

## 10. Algorithm References

> No独立 algorithm references in this context.

---

## 11. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Passenger | $P$ | A traveler with amount and multi-leg flight list |
| Flight Passenger | $FP$ | Association between a passenger and a specific flight |
| Cancel | $c$ | Number of cancelled passengers |
| Class Change | $s$ | Number of passengers changing class |
| Flight Change | $r$ | Number of passengers changing flight |
| Route | route | Sequence of airports visited by a passenger |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Separate cancel/change modeling | Unified recovery variable | Different business semantics and penalty weights | - |
| Route-level cancel linkage | Independent per-leg cancellation | Passenger experience: partial cancellation is meaningless | - |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1 | Initial implementation | Basic passenger domain modeling |
