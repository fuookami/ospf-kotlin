# Network Scheduling Allocation Context

[简体中文](README_ch.md)

This module provides a transport/allocation business-input layer over the existing network Flow and Network Design contexts. It translates locations, directed services, shipment requests, shared quotas, optional hubs, and capacity levels into the lower-level immutable problem snapshots.

## Public API

- `AllocationProblem<V>` is the immutable input snapshot. `V` remains a `RealNumber<V>`, and business amounts, capacities, costs, and quotas use `Quantity<V>`.
- `AllocationLocation<V>` provides a stable network location and an optional integer layer. When both endpoints have layers, a service may only go from a lower/equal layer to a higher/equal layer; increasing-layer routes may still transfer through intermediate locations.
- `AllocationService<V>` defines an operating cost per request unit and capacity bounds. Optional `AllocationCapacityLevel<V>` choices map to design decisions; capacity is the resulting total capacity, not an increment. A service with no levels is fixed and always available; an unrequired service with levels may be closed.
- `AllocationRequest<V>` defines an origin, destination, positive quantity, optional compatible services, per-service unit-cost overrides, and an optional integer flow step. Each request becomes its own flow commodity.
- `AllocationQuota<V>` defines a shared resource row across request-service flows. `AllocationProblem.serviceUsage` can override the default one-unit service-capacity consumption when request and service-capacity dimensions differ.
- `AllocationHub<V>` maps to an optional facility. Its `quotaIds` are shared rows gated by opening the hub; service flow on an incident arc is also gated by facility activation.
- `AllocationContext.create(...)` validates and builds a fresh `NetworkDesignContext`. `register(...)` adds it to a `MetaModel`, and `extractSolution(...)` returns request shipments, resource use, operating/construction costs, selected service levels, and open hubs.

## Cost and quantity semantics

Service and request-specific operating costs are per one unit of that request. A request override takes precedence over the corresponding service cost. All operating and construction costs are normalized to `AllocationProblem.costUnit` by the network-scheduling value adapter at registration/extraction boundaries.

Capacities and shared quota bounds are physical quantities. The default arc capacity usage assumes one capacity unit is consumed by one request unit. If those units have different dimensions, provide a positive `serviceUsage` coefficient expressed in capacity units per request unit. Quota usage is likewise expressed in quota units per request unit. Incompatible quantities are reported through `Ret` errors rather than mixed as raw values.

An integer step means each request-service flow is an integer multiple of that step. It does **not** make an entire request indivisible: one request may be split across compatible services and intermediate locations, provided conservation, capacity, and quota constraints hold.

## Example

```kotlin
val problem = AllocationProblem(
    locations = listOf(
        AllocationLocation(NetworkNodeId("warehouse"), layer = 0),
        AllocationLocation(NetworkNodeId("hub"), layer = 1),
        AllocationLocation(NetworkNodeId("store"), layer = 2)
    ),
    services = listOf(
        AllocationService(
            id = NetworkArcId("warehouse-hub"),
            from = NetworkNodeId("warehouse"),
            to = NetworkNodeId("hub"),
            cost = Quantity(Flt64(1.0), NoneUnit),
            capacity = capacity
        ),
        AllocationService(
            id = NetworkArcId("hub-store"),
            from = NetworkNodeId("hub"),
            to = NetworkNodeId("store"),
            cost = Quantity(Flt64(1.0), NoneUnit),
            capacity = capacity
        )
    ),
    requests = listOf(
        AllocationRequest(
            id = "order-42",
            origin = NetworkNodeId("warehouse"),
            destination = NetworkNodeId("store"),
            quantity = Quantity(Flt64(12.0), NoneUnit),
            integerStep = Quantity(Flt64(1.0), NoneUnit)
        )
    ),
    zero = Flt64.zero
)
val context = AllocationContext.create(problem, Flt64NetworkSchedulingSolverValueAdapter).value
```

The context is isolated per instance: create one context per model registration. It reuses the Flow and Network Design kernels and does not depend on Gantt scheduling or a native solver.
