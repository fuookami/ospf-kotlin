# Bunch Compilation Domain Model

:us: English | :cn: [简体中文](domain-model_ch.md)

[toc]

## 1. Overview

Compiles flight task bunches into the column generation optimization model, managing the registration of task-time, flow, fleet-balance, flight-link, and flight-capacity constraints and incremental column addition.

### 1. Dependent Contexts

1. **task** (Flight Task)
2. **rule** (Rule)
3. **framework (gantt_scheduling)**

---

## 2. Concepts / Entities

### 1. Compilation

The set of decision variables for flight task bunches in column generation, specialized as `BunchCompilation<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>`.

**$x_{b}^{(k)}$** : Decision variable for bunch $b$ in iteration $k$, taking values 0 or 1, indicating whether bunch $b$ is selected.
**$y_{i}$** : Auxiliary decision variable for flight task $i$, used in link and fleet balance constraints.
**$z_{a}$** : Auxiliary decision variable for aircraft $a$, used in fleet balance constraints.

### 2. Flight Link (FlightLink)

Represents a connection between two consecutive unrecovered flight legs with a split cost.

**$\text{prevTask}_{l}$** : Predecessor task of link $l$.
**$\text{succTask}_{l}$** : Successor task of link $l$.
**$\text{splitCost}_{l}$** : Split cost of link $l$.

### 3. Fleet Balance CheckPoint

A combination of airport and aircraft minor type, used to track aircraft distribution across airports.

**$\text{airport}_{c}$** : Airport of checkpoint $c$.
**$\text{aircraftMinorType}_{c}$** : Aircraft minor type of checkpoint $c$.

### 4. Flight Capacity

Tracks passenger and cargo capacity expressions across flight task bunches.

**$\text{passenger}_{i,cls}$** : Passenger capacity expression for flight task $i$ at class $cls$.
**$\text{cargo}_{i}$** : Cargo capacity expression for flight task $i$.

---

## 3. Variables

### 1. Decision Variables

**$x_{b}^{(k)}$** : Selection variable for bunch $b$ in iteration $k$, dimensionless, domain $\{0, 1\}$, indicates whether bunch $b$ is selected for the recovery plan, $\forall b \in B^{(k)}$.

**$y_{i}$** : Link auxiliary variable for task $i$, dimensionless, domain $\{0, 1\}$, indicates whether task $i$ is covered by a selected bunch, $\forall i \in I$.

**$z_{a}$** : Fleet balance auxiliary variable for aircraft $a$, dimensionless, domain $\{0, 1\}$, indicates whether aircraft $a$ is used, $\forall a \in A$.

### 2. Auxiliary Variables

**$\text{link\_slack}_{l}$** : Slack variable for link $l$, domain $[0, +\infty)$, penalizes links not covered by any selected bunch, $\forall l \in L$.

**$\text{fleet\_slack}_{c}$** : Fleet balance slack variable for checkpoint $c$, domain $[0, +\infty)$, penalizes aircraft distribution deviations, $\forall c \in C$.

---

## 4. Predicates

### 1. Task Type

**isFlight** : Task $i$ is of flight type (Flight or VirtualFlight).
**isRecoveryNeeded** : Task $i$ requires recovery within the recovery time window.

### 2. Capacity Type

**hasPassenger** : Aircraft of flight task $i$ has passenger capacity.
**hasCargo** : Aircraft of flight task $i$ has cargo capacity.

---

## 5. Sets

### 1. Bunches

**$B$** : Universal set of all generated flight task bunches.

**$B^{(k)}$** : Subset of bunches generated in iteration $k$.
**$B_{a}$** : Subset of bunches assigned to aircraft $a$, $\forall a \in A$.
**$B_{i}$** : Subset of bunches containing task $i$, $\forall i \in I$.

### 2. Tasks

**$I$** : Universal set of all flight tasks.

**$I^{R}$** : Subset of tasks requiring recovery.
**$I^{F}$** : Subset of flight-type tasks.

### 3. Links

**$L$** : Universal set of all flight links.

**$L^{C}$** : Subset of connecting links.
**$L^{S}$** : Subset of stopover links.
**$L^{I}$** : Subset of connection-time-ignoring links.

### 4. Checkpoints

**$C$** : Universal set of all fleet balance checkpoints (airport × minor type combinations).

---

## 6. Intermediate Values

### 1. Link Expression

**Description**: The number of selected bunches covering link $l$.

$$
\text{link}_{l} = \sum_{b \in B : b \supset l} x_{b}^{(k)}, \; \forall l \in L
$$

### 2. Fleet Balance Expression

**Description**: The number of aircraft arriving at checkpoint $c$.

$$
\text{fleet}_{c} = \sum_{a \in A_{c}} z_{a}, \; \forall c \in C
$$

### 3. Passenger Capacity Expression

**Description**: Total passenger capacity for flight task $i$ at class $cls$.

$$
\text{passenger\_capacity}_{i,cls} = \sum_{b \in B_{i}} \text{cap}(b, i, cls) \cdot x_{b}^{(k)}, \; \forall i \in I^{F}, \forall cls \in CLS
$$

### 4. Cargo Capacity Expression

**Description**: Total cargo capacity for flight task $i$.

$$
\text{cargo\_capacity}_{i} = \sum_{b \in B_{i}} \text{cap}(b, i) \cdot x_{b}^{(k)}, \; \forall i \in I^{F}
$$

---

## 7. Assertions

### 1. Link Coverage Consistency

**Description**: Each link's coverage count must be consistent with the task decision variables of bunches containing it.

$$
\forall l \in L \; (\text{link}_{l} = \sum_{b \in B : b \supset l} x_{b})
$$

### 2. Fleet Balance Consistency

**Description**: The aircraft count at each checkpoint must match the original plan.

$$
\forall c \in C \; (\text{fleet}_{c} = \text{expected\_amount}_{c})
$$

---

## 8. Constraints

### 1. Task Coverage Constraint

**[CN]**: 任务覆盖约束
**描述**：每个需要恢复的航班任务必须被恰好一个选中的束覆盖。

$$
s.t. \quad \sum_{b \in B_{i}} x_{b} = 1, \; \forall i \in I^{R}
$$

### 2. Link Slack Constraint

**[CN]**: 链接松弛约束
**描述**：链接的覆盖数量加上松弛变量应大于等于阈值。

$$
s.t. \quad \text{link}_{l} + \text{link\_slack}_{l} \geq 1, \; \forall l \in L
$$

### 3. Fleet Balance Constraint

**[CN]**: 车队平衡约束
**描述**：到达每个检查点的飞机数量加上松弛变量应等于预期数量。

$$
s.t. \quad \text{fleet}_{c} + \text{fleet\_slack}_{c} = \text{expected\_amount}_{c}, \; \forall c \in C
$$

---

## 9. Objective Function

**Description**: Minimize total recovery cost including bunch costs and slack penalties.

$$
\min \sum_{b \in B} \text{cost}(b) \cdot x_{b} + \sum_{l \in L} \lambda_{l} \cdot \text{link\_slack}_{l} + \sum_{c \in C} \mu_{c} \cdot \text{fleet\_slack}_{c}
$$

---

## 10. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| Threshold Slack | `exampleThresholdSlack` | Section 3 Auxiliary Variables | Threshold slack function for link and fleet balance |

---

## 11. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Bunch | $B$ | An ordered sequence of flight tasks assigned to a single aircraft |
| Compilation | Compilation | The set of decision variables for column generation |
| CheckPoint | $C$ | A combination of airport and aircraft minor type |
| Link | $L$ | A connection between two consecutive flight legs |
| Split Cost | splitCost | Cost allocation for a link |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Use threshold slack instead of hard constraints | Hard constraints, linear relaxation | Allows infeasible solutions with penalty, improving solving flexibility | - |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1 | Initial implementation | Basic column generation compilation |
