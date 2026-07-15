# Route Context

> English | [中文](README_ch.md)

## Overview

The Route Context is the foundational bounded context in the Demo1 framework demonstration. It is responsible for building the network topology graph from input data and managing service-to-node routing assignments. This context provides the graph structure, service definitions, and assignment decision variables to the Bandwidth Context, serving as the starting point of the entire optimization pipeline.

## Responsibilities

- Build the network graph (nodes and edges) from input data
- Define services (with capacity and cost properties)
- Define service-to-node assignment decision variables
- Enforce routing constraints (node assignment limit, service assignment limit)
- Minimize total service assignment cost

## Dependent Contexts

None. This context is the entry point of the Demo1 optimization pipeline.

## Directory Structure

```
route_context/
├── RouteContext.kt              # Context entry point, builds graph and manages model construction
├── Aggregation.kt               # Aggregates graph, services, and assignment variables for unified registration
├── model/
│   ├── Graph.kt                 # Network graph structure (Node, Edge, Graph)
│   ├── Service.kt               # Service definition (capacity, cost)
│   └── Assignment.kt            # Assignment decision variables (x) and intermediate symbols
└── service/
    ├── PipelineListGenerator.kt # Generates the pipeline list of constraints and objectives
    └── limits/
        ├── NodeAssignmentConstraint.kt    # Node assignment constraint
        ├── ServiceAssignmentConstraint.kt # Service assignment constraint
        └── ServiceCostObjective.kt        # Service cost objective function
```

## Core Concepts

### Entities

#### Node

Network nodes, divided into two types:

- **NormalNode** — A transit node that can carry service traffic
- **ClientNode** — A terminal node that consumes bandwidth, with a specific demand value

#### Edge

A directed edge between two nodes, with:
- **maxBandwidth** — Maximum bandwidth capacity
- **costPerBandwidth** — Per-unit bandwidth cost

#### Service

A service that can be routed through the network, with:
- **capacity** — Service capacity limit
- **cost** — Per-use cost

### Decision Variables

- **x[node, service]** — Binary variable indicating whether `service` is assigned to `node`

### Intermediate Symbols

- **nodeAssignment[node]** — Number of services assigned to `node` (sum across all services)
- **serviceAssignment[service]** — Number of nodes that `service` is assigned to (sum across all nodes)

### Constraints

| Constraint | Description |
|------------|-------------|
| NodeAssignmentConstraint | Each normal node can be assigned at most one service |
| ServiceAssignmentConstraint | Each service can be assigned to at most one node |

### Objective Function

Minimize total service assignment cost: $\min \sum_{s \in S} cost_s \cdot serviceAssignment_s$

## Workflow

1. **init** — Build nodes, edges, and services from input data, create the network graph and assignment model
2. **register** — Register assignment decision variables and intermediate symbols with the optimization model
3. **construct** — Generate constraint and objective pipelines, inject them into the optimization model

## Graph Construction Rules

- Each undirected input edge is converted into two directed edges (bidirectional)
- Client nodes are connected to their associated normal nodes via zero-cost edges
- The bandwidth of a client connection edge equals the client's demand

## Usage Example

```kotlin
val routeContext = RouteContext()
routeContext.init(input)
routeContext.register(model)
routeContext.construct(model)

// The route context's aggregation data is available for the bandwidth context
val aggregation = routeContext.aggregation
```
