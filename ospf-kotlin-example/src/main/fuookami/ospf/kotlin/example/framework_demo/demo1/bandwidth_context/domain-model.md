# Bandwidth Context Domain Model

> English | [中文](domain-model_ch.md)

[toc]

## 1. Overview

The Bandwidth Context is responsible for allocating bandwidth resources to services across the network topology, achieving optimal bandwidth allocation by minimizing total bandwidth cost.

### 1. Dependent Contexts

1. Route Context — Provides the network graph structure, service definitions, and routing assignment variables

---

## 2. Concepts / Entities

### 1. Edge

A directed connection between two nodes in the network, with bandwidth capacity and per-unit cost.

**$from_{e}$** : The source node of edge $e$.
**$to_{e}$** : The destination node of edge $e$.
**$maxBandwidth_{e}$** : The maximum bandwidth capacity of edge $e$, a dimensionless integer quantity.
**$costPerBandwidth_{e}$** : The per-unit bandwidth cost of edge $e$, a dimensionless integer quantity.

### 2. Service

A logical channel that can be routed through the network, with a capacity limit and per-use cost.

**$capacity_{s}$** : The capacity limit of service $s$.
**$cost_{s}$** : The per-use cost of service $s$.

### 3. Node

A vertex in the network, classified as either a transit node (NormalNode) or a terminal node (ClientNode).

**$id_{n}$** : The unique identifier of node $n$.
**$demand_{n}$** : The bandwidth demand of node $n$ (valid only for terminal nodes).
**$edges_{n}$** : The set of edges connected to node $n$.

---

## 3. Variables

### 1. Decision Variables

**$y_{e,s}$** : Bandwidth allocated to service $s$ on edge $e$, dimensionless integer, domain is $[0, maxBandwidth_{e}]$, representing the bandwidth occupied by service $s$ on edge $e$, $\forall e \in E$, $\forall s \in S$.

### 2. Auxiliary Variables

None.

---

## 4. Predicates

### 1. Node Type

**normal** : The node is a transit node (NormalNode).
**client** : The node is a terminal node (ClientNode).

### 2. Edge Type

**from_normal** : The source node of the edge is a transit node.

---

## 5. Sets

### 1. Nodes

**$N$** : The set of all nodes in the network.

**$N^{normal}$** : Subset satisfying predicate normal, i.e., all transit nodes.
**$N^{client}$** : Subset satisfying predicate client, i.e., all terminal nodes.

### 2. Edges

**$E$** : The set of all directed edges in the network.

**$E^{normal}$** : Subset satisfying predicate from_normal, i.e., edges whose source node is a transit node.

### 3. Services

**$S$** : The set of all available services.

---

## 6. Intermediate Values

### 1. Edge Total Bandwidth (bandwidth)

**Description**: The sum of bandwidth allocated to all services on each normal edge.

$$
bandwidth_{e} = \sum_{s \in S} y_{e,s}, \; \forall e \in E^{normal}
$$

### 2. Service In-Degree Bandwidth (inDegree/service)

**Description**: The in-degree bandwidth of each service at each node, i.e., the sum of that service's bandwidth on all edges pointing to the node.

$$
inDegree_{s,n} = \sum_{e \in E: to_{e}=n} y_{e,s}, \; \forall s \in S, \; \forall n \in N
$$

### 3. Service Out-Degree Bandwidth (outDegree/service)

**Description**: The out-degree bandwidth of each service at each transit node, i.e., the sum of that service's bandwidth on all edges originating from the node.

$$
outDegree_{s,n} = \sum_{e \in E: from_{e}=n} y_{e,s}, \; \forall s \in S, \; \forall n \in N^{normal}
$$

### 4. Service Out-Flow (outFlow/service)

**Description**: The net out-flow of each service at each transit node, computed as out-degree minus in-degree.

$$
outFlow_{s,n} = outDegree_{s,n} - inDegree_{s,n}, \; \forall s \in S, \; \forall n \in N^{normal}
$$

### 5. Node Aggregated In-Degree Bandwidth (inDegree/node)

**Description**: The sum of in-degree bandwidth across all services at each node.

$$
inDegree_{n} = \sum_{s \in S} inDegree_{s,n}, \; \forall n \in N
$$

### 6. Node Aggregated Out-Degree Bandwidth (outDegree/node)

**Description**: The sum of out-degree bandwidth across all services at each transit node.

$$
outDegree_{n} = \sum_{s \in S} outDegree_{s,n}, \; \forall n \in N^{normal}
$$

### 7. Node Aggregated Out-Flow (outFlow/node)

**Description**: The sum of net out-flow across all services at each transit node.

$$
outFlow_{n} = \sum_{s \in S} outFlow_{s,n}, \; \forall n \in N^{normal}
$$

### 8. Node Maximum Out-Degree Capacity (maxOutDegree)

**Description**: The maximum outgoing bandwidth capacity of a node, computed as the sum of maximum bandwidth of all outgoing edges.

$$
maxOutDegree_{n} = \sum_{e \in E: from_{e}=n} maxBandwidth_{e}, \; \forall n \in N^{normal}
$$

---

## 7. Assertions

### 1. Terminal Nodes Have Zero Bandwidth Allocation

**Description**: The bandwidth allocation on all services at terminal nodes (ClientNode) is zero.

$$
\forall n \in N^{client}, \; \forall s \in S, \; \forall e \in E: from_{e}=n \; (y_{e,s} = 0)
$$

### 2. Non-Normal Edges Have Zero Bandwidth

**Description**: On edges whose source node is not a transit node, the bandwidth allocation for all services is zero.

$$
\forall e \in E \setminus E^{normal}, \; \forall s \in S \; (y_{e,s} = 0)
$$

---

## 8. Constraints

### 1. Edge Bandwidth Constraint

**[CN]**: 边带宽约束
**Description**: When a service is not assigned to an edge, its bandwidth on that edge must be zero; when assigned, bandwidth must not exceed the edge's maximum capacity.

$$
s.t. \quad (1 - x_{n,s}) \cdot maxBandwidth_{e} + y_{e,s} \leq maxBandwidth_{e}, \; \forall e \in E^{normal}, \; \forall s \in S
$$

where $x_{n,s}$ is the assignment variable from the Route Context, and $n = from_{e}$.

### 2. Demand Constraint

**[CN]**: 需求满足约束
**Description**: Each terminal node's in-degree bandwidth must meet its bandwidth demand.

$$
s.t. \quad inDegree_{n} \geq demand_{n}, \; \forall n \in N^{client}
$$

### 3. Service Capacity Constraint

**[CN]**: 服务容量约束
**Description**: When a service is assigned to a node, the service's out-flow at that node must not exceed the service capacity.

$$
s.t. \quad capacity_{s} \cdot (1 - x_{n,s}) + outFlow_{s,n} \leq capacity_{s}, \; \forall n \in N^{normal}, \; \forall s \in S
$$

### 4. Transfer Node Bandwidth Constraint

**[CN]**: 传输节点带宽约束
**Description**: When a transit node is assigned, its total out-flow must not exceed the node's maximum out-degree capacity.

$$
s.t. \quad maxOutDegree_{n} \cdot (1 - x_{n}) + outFlow_{n} \leq maxOutDegree_{n}, \; \forall n \in N^{normal}
$$

where $x_{n}$ is the node assignment variable from the Route Context.

---

## 9. Objective Function

**Description**: Minimize the total bandwidth cost across all normal edges.

$$
\min \sum_{e \in E^{normal}} costPerBandwidth_{e} \cdot bandwidth_{e}
$$

---

## 10. Algorithm References

| Algorithm Name | File Path | Referenced In | Brief Description |
|----------------|-----------|---------------|-------------------|
| DFS Path Extraction | `service/SolutionAnalyzer.kt` | Section 6 | Extracts service paths from the solved model via depth-first search |

---

## 11. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Edge | $e$ | A directed connection between two nodes |
| Service | $s$ | A logical channel that can be routed through the network |
| Transit Node | $n$ (NormalNode) | An intermediate node that can carry service traffic |
| Terminal Node | $n$ (ClientNode) | An end node that consumes bandwidth |
| Bandwidth | $y_{e,s}$ | The amount of bandwidth allocated to a service on an edge |
| In-Degree | $inDegree$ | Total bandwidth flowing into a node |
| Out-Degree | $outDegree$ | Total bandwidth flowing out of a node |
| Out-Flow | $outFlow$ | Net outgoing bandwidth of a node (out-degree - in-degree) |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Integer variables for edge bandwidth | Continuous variables | Simplifies solving; aligns with discrete nature of practical bandwidth allocation | 2024 |
| Intermediate symbols via flatMap | Direct polynomial construction | Declarative API provided by the framework; improves code readability | 2024 |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial implementation | Demo1 framework demonstration |
