# Loading Effectiveness Domain Model

[toc]

## 1. Overview

Manages loading effectiveness constraints for operational efficiency — including trailer loading, sequential loading, transfer adjacency, and source/destination grouping.

### 1. Dependent Contexts

1. Aircraft
2. Stowage

---

## 2. Concepts / Entities

### 1. Advice Loading

Suggested loading amounts and weights per position for predistribution mode.

**$adviceAmount_{j}$** : Advice load amount at position $j$.
**$adviceWeight_{j}$** : Advice load weight at position $j$.

### 2. Transfer Adjacent Loading

Same-source/same-destination adjacency constraints for transfer efficiency.

**$adjacentPositions$** : List of adjacent position pairs.
**$sources$** : List of source stations.
**$destinations$** : List of destination stations.

### 3. Sequential Loading

Sequential loading constraints based on position ordering.

**$orderedPositions$** : List of ordered position pairs.

### 4. Trailer Loading

Trailer change and circling constraints for full-load mode.

**$trailers$** : List of trailers.

---

## 3. Variables

This context reuses decision variables from the stowage context and does not define independent decision variables.

---

## 4. Constraints

### 1. Same Source Adjacent Limit

**[CN]**: 同源邻接限制
**Description**: Same-source items should be loaded in adjacent positions.

$$
s.t. \quad source_i = source_k \rightarrow adjacent(x_{ij}, x_{kl}), \; \forall i, k \in Items_{sameSource}
$$

### 2. Same Destination Adjacent Limit

**[CN]**: 同目的地邻接限制
**Description**: Same-destination items should be loaded in adjacent positions.

$$
s.t. \quad dest_i = dest_k \rightarrow adjacent(x_{ij}, x_{kl}), \; \forall i, k \in Items_{sameDest}
$$

### 3. Trailer Change Limit

**[CN]**: 拖车变更限制
**Description**: Minimize trailer changes during loading.

$$
\min \sum trailerChanges
$$

---

## 5. Objective Function

Minimize loading operational cost.

$$
\min loadingOperationalCost
$$

---

## 6. Ubiquitous Language

| Term | Symbol | English | Definition |
|------|--------|---------|------------|
| 建议装载 | AdviceLoading | Advice Loading | Advice load amount per position |
| 转运邻接 | TransferAdjacent | Transfer Adjacent | Adjacency requirement for transfer cargo |
| 顺序装载 | SequentialLoading | Sequential Loading | Order-based loading constraints |
| 拖车装载 | TrailerLoading | Trailer Loading | Trailer-related loading constraints |

---

## 7. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Adjacency definition | Loading order vs Physical position | Loading order matches operational reality | 2024 |
