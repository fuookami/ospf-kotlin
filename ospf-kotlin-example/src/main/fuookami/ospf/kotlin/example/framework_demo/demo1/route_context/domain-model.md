# Route Context Domain Model

> English | [中文](domain-model_ch.md)

[toc]

## 1. Overview

The Route Context is responsible for building the network topology graph from input data and managing service-to-node routing assignments. It serves as the starting point of the entire optimization pipeline.

### 1. Dependent Contexts

None.

---

## 2. Concepts / Entities

### 1. Node

A vertex in the network, the fundamental unit of the graph structure.

**$id_{n}$** : The unique identifier of node $n$, a dimensionless integer.
**$edges_{n}$** : The list of edges connected to node $n$.

#### 1.1 Transit Node (NormalNode)

An intermediate node that can carry service traffic, without bandwidth demand.

#### 1.2 Terminal Node (ClientNode)

An end node that consumes bandwidth from the network, with a specific demand.

**$demand_{n}$** : The bandwidth demand of terminal node $n$, a dimensionless integer.

### 2. Edge

A directed connection between two nodes.

**$from_{e}$** : The source node of edge $e$.
**$to_{e}$** : The destination node of edge $e$.
**$maxBandwidth_{e}$** : The maximum bandwidth capacity of edge $e$, a dimensionless integer.
**$costPerBandwidth_{e}$** : The per-unit bandwidth cost of edge $e$, a dimensionless integer.

### 3. Service

A logical channel that can be routed through the network.

**$id_{s}$** : The unique identifier of service $s$.
**$capacity_{s}$** : The capacity limit of service $s$, a dimensionless integer.
**$cost_{s}$** : The per-use cost of service $s$, a dimensionless integer.

### 4. Graph

A container for the network structure holding all nodes and edges.

**$nodes$** : The list of all nodes in the graph.
**$edges$** : The list of all directed edges in the graph.

---

## 3. Variables

### 1. Decision Variables

**$x_{n,s}$** : Binary variable indicating whether service $s$ is assigned to node $n$, domain is $\{0, 1\}$, 1 means assigned, 0 means not assigned, $\forall n \in N^{normal}$, $\forall s \in S$.

### 2. Auxiliary Variables

None.

---

## 4. Predicates

### 1. Node Type

**normal** : The node is a transit node (NormalNode).
**client** : The node is a terminal node (ClientNode).

---

## 5. Sets

### 1. Nodes

**$N$** : The set of all nodes in the network.

**$N^{normal}$** : Subset satisfying predicate normal, i.e., all transit nodes.
**$N^{client}$** : Subset satisfying predicate client, i.e., all terminal nodes.

### 2. Edges

**$E$** : The set of all directed edges in the network.

### 3. Services

**$S$** : The set of all available services.

---

## 6. Intermediate Values

### 1. Node Assignment Count (nodeAssignment)

**Description**: The number of services assigned to each transit node, computed as the sum of all service assignment variables.

$$
nodeAssignment_{n} = \sum_{s \in S} x_{n,s}, \; \forall n \in N^{normal}
$$

### 2. Service Assignment Count (serviceAssignment)

**Description**: The number of nodes that each service is assigned to, computed as the sum of that service's assignment variables across all transit nodes.

$$
serviceAssignment_{s} = \sum_{n \in N^{normal}} x_{n,s}, \; \forall s \in S
$$

---

## 7. Assertions

### 1. Terminal Nodes Have No Service Assignment

**Description**: The assignment variables for all services at terminal nodes (ClientNode) are zero.

$$
\forall n \in N^{client}, \; \forall s \in S \; (x_{n,s} = 0)
$$

---

## 8. Constraints

### 1. Node Assignment Constraint

**[CN]**: 节点分配约束
**Description**: Each transit node can be assigned at most one service.

$$
s.t. \quad nodeAssignment_{n} \leq 1, \; \forall n \in N^{normal}
$$

### 2. Service Assignment Constraint

**[CN]**: 服务分配约束
**Description**: Each service can be assigned to at most one node.

$$
s.t. \quad serviceAssignment_{s} \leq 1, \; \forall s \in S
$$

---

## 9. Objective Function

**Description**: Minimize the total service assignment cost.

$$
\min \sum_{s \in S} cost_{s} \cdot serviceAssignment_{s}
$$

---

## 10. Algorithm References

None.

---

## 11. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Node | $n$ | A vertex in the network |
| Transit Node | $n$ (NormalNode) | An intermediate node that can carry service traffic |
| Terminal Node | $n$ (ClientNode) | An end node that consumes bandwidth |
| Edge | $e$ | A directed connection between two nodes |
| Service | $s$ | A logical channel that can be routed through the network |
| Assignment | $x_{n,s}$ | Binary decision on whether a service is assigned to a node |
| Capacity | $capacity_{s}$ | The bandwidth capacity limit of a service |
| Demand | $demand_{n}$ | The bandwidth demand of a terminal node |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Convert each undirected edge to two directed edges | Keep undirected edges | Simplifies model construction; facilitates directional constraint expression | 2024 |
| Connect client nodes via zero-cost edges | Direct association to normal nodes | Unified graph structure; facilitates edge traversal in the bandwidth context | 2024 |
| Number of services is half the number of normal nodes | Dynamic configuration | Simplified Demo1 setting; should be configurable in production | 2024 |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial implementation | Demo1 framework demonstration |
