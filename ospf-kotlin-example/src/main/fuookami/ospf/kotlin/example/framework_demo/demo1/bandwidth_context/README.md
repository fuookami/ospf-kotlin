# Bandwidth Context

> English | [中文](README_ch.md)

## Overview

The Bandwidth Context is one of the core bounded contexts in the Demo1 framework demonstration. It is responsible for allocating bandwidth resources to services across the network topology. Built on top of the network graph and service definitions provided by the Route Context, this context constructs a bandwidth optimization model that minimizes total bandwidth cost while satisfying all demand and capacity constraints.

## Responsibilities

- Define edge bandwidth decision variables (bandwidth allocation per edge per service)
- Compute service-level and node-level intermediate bandwidth metrics (in-degree, out-degree, out-flow)
- Enforce bandwidth constraints (edge capacity, demand satisfaction, service capacity, transfer node capacity)
- Minimize total bandwidth cost
- Extract service paths from the solved model

## Dependent Contexts

- **Route Context** — Provides the network graph structure (nodes, edges), service definitions, and routing assignment variables

## Directory Structure

```
bandwidth_context/
├── BandwidthContext.kt          # Context entry point, orchestrates init/register/construct/analyze
├── Aggregation.kt               # Aggregates edge/service/node bandwidth models for unified registration
├── model/
│   ├── EdgeBandwidth.kt         # Edge bandwidth decision variables (y) and intermediate symbols (bandwidth)
│   ├── ServiceBandwidth.kt      # Service-level in-degree/out-degree/out-flow intermediate symbols
│   └── NodeBandwidth.kt         # Node-level aggregated in-degree/out-degree/out-flow intermediate symbols
└── service/
    ├── PipelineListGenerator.kt # Generates the pipeline list of constraints and objectives
    ├── SolutionAnalyzer.kt      # Extracts service paths from solved model via DFS
    └── limits/
        ├── EdgeBandwidthConstraint.kt           # Edge bandwidth constraint
        ├── DemandConstraint.kt                  # Demand satisfaction constraint
        ├── ServiceCapacityConstraint.kt         # Service capacity constraint
        ├── TransferNodeBandwidthConstraint.kt   # Transfer node bandwidth constraint
        └── BandwidthCostObjective.kt            # Bandwidth cost objective function
```

## Core Concepts

### Decision Variables

- **y[edge, service]** — Bandwidth allocated to `service` on `edge`, non-negative integer, upper bounded by the edge's maximum bandwidth

### Intermediate Symbols

- **bandwidth[edge]** — Total bandwidth on an edge (sum across all services)
- **inDegree/service[service, node]** — In-degree bandwidth of `service` at `node`
- **outDegree/service[service, node]** — Out-degree bandwidth of `service` at `node`
- **outFlow/service[service, node]** — Net out-flow of `service` at `node` (out-degree - in-degree)
- **inDegree/node[node]** — Aggregated in-degree bandwidth at `node` (sum across all services)
- **outDegree/node[node]** — Aggregated out-degree bandwidth at `node`
- **outFlow/node[node]** — Aggregated net out-flow at `node`

### Constraints

| Constraint | Description |
|------------|-------------|
| EdgeBandwidthConstraint | When a service is not assigned to an edge, its bandwidth on that edge must be zero |
| DemandConstraint | Each client node's in-degree bandwidth must meet its demand |
| ServiceCapacityConstraint | Each normal node's service out-flow must not exceed the service capacity |
| TransferNodeBandwidthConstraint | Each normal node's total out-flow must not exceed its maximum bandwidth capacity |

### Objective Function

Minimize total bandwidth cost across all normal edges: $\min \sum_{e \in E} cost_e \cdot bandwidth_e$

## Workflow

1. **init** — Retrieve graph and services from the route context, create edge/service/node bandwidth models
2. **register** — Register decision variables and intermediate symbols with the optimization model
3. **construct** — Generate constraint and objective pipelines, inject them into the optimization model
4. **analyze** — Extract service paths from the solver result

## Usage Example

```kotlin
val routeContext = RouteContext()
routeContext.init(input)
routeContext.register(model)
routeContext.construct(model)

val bandwidthContext = BandwidthContext(routeContext)
bandwidthContext.init(input)
bandwidthContext.register(model)
bandwidthContext.construct(model)

// Analyze results after solving
val paths = bandwidthContext.analyze(model, solverResult)
```
