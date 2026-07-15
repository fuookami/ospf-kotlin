# demo1 — Network Routing with Bandwidth Constraints

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`demo1` demonstrates the **Shortest Service Path (SSP)** problem: assign routes to network services while respecting edge and node bandwidth capacities. It uses two domain contexts — `route_context` for route assignment and `bandwidth_context` for bandwidth allocation — to build a linear optimization model solved by SCIP.

## Scope

- Model a directed network graph with edges, nodes, and services.
- Assign each service to a path (sequence of edges) minimizing total cost.
- Enforce bandwidth constraints on edges and transfer nodes.
- Respect service demand and capacity limits.

## Module Structure

| Package | Responsibility |
| --- | --- |
| `route_context` | Route assignment: graph model, service paths, assignment constraints |
| `bandwidth_context` | Bandwidth allocation: edge/node/service bandwidth, cost objective |
| `infrastructure` | DTO definitions for input/output |

### route_context

| File | Description |
| --- | --- |
| `model/Graph.kt` | Network graph: nodes and edges |
| `model/Service.kt` | Service definition with source, sink, and demand |
| `model/Assignment.kt` | Route assignment decision variables |
| `service/limits/NodeAssignmentConstraint.kt` | Flow conservation at each node |
| `service/limits/ServiceAssignmentConstraint.kt` | Each service must be assigned exactly one path |
| `service/limits/ServiceCostObjective.kt` | Minimize total routing cost |
| `service/PipelineListGenerator.kt` | Pipeline for constraint registration |

### bandwidth_context

| File | Description |
| --- | --- |
| `model/EdgeBandwidth.kt` | Edge bandwidth capacity and usage |
| `model/NodeBandwidth.kt` | Transfer node bandwidth capacity |
| `model/ServiceBandwidth.kt` | Service bandwidth demand |
| `service/limits/EdgeBandwidthConstraint.kt` | Edge bandwidth capacity constraint |
| `service/limits/TransferNodeBandwidthConstraint.kt` | Transfer node capacity constraint |
| `service/limits/ServiceCapacityConstraint.kt` | Service capacity constraint |
| `service/limits/DemandConstraint.kt` | Service demand satisfaction |
| `service/limits/BandwidthCostObjective.kt` | Bandwidth cost objective |
| `service/SolutionAnalyzer.kt` | Extract route assignments from solution |

## Architecture

```
demo1/
  Application.kt          -- Entry point: SSP class with invoke()
  Interface.kt             -- Public interface definitions
  infrastructure/DTO.kt    -- Input/Output DTOs
  route_context/           -- Route assignment domain
    RouteContext.kt         -- Context: init, register, construct, analyze
    Aggregation.kt          -- Domain model state
    model/                  -- Graph, Service, Assignment
    service/limits/         -- Constraints and objectives
  bandwidth_context/       -- Bandwidth allocation domain
    BandwidthContext.kt     -- Context: init, register, construct, analyze
    Aggregation.kt          -- Domain model state
    model/                  -- Edge/Node/Service bandwidth
    service/limits/         -- Constraints and objectives
```

## Usage

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo1.SSP

suspend fun main() {
    val ssp = SSP()
    val input = Input(/* network graph, services, bandwidth params */)
    val result = ssp(input)
    when (result) {
        is Ok -> println("Solution: ${result.value}")
        is Failed -> println("Failed: ${result.error}")
        is Fatal -> println("Fatal: ${result.errors}")
    }
}
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
