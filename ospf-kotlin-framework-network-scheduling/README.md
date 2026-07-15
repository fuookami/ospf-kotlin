# ospf-kotlin-framework-network-scheduling

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-framework-network-scheduling` is the planned network scheduling framework module. It is currently a Maven module shell with no tracked Kotlin implementation.

## Scope

The module is reserved for reusable network scheduling kernels:

1. Network nodes, arcs, routes, flows, capacities, and travel-time models.
2. Quantity-aware cost, demand, capacity, throughput, and duration values.
3. Model components and pipelines for future network LP/MIP or decomposition workflows.

Project-specific DTOs, tenant context, heartbeat logic, and solver plugin selection belong in downstream adapters.

## Public API

No stable runtime public API exists yet. Future APIs should expose generic graph and quantity-aware domain types.

## Modeling Extensions

Future implementation should keep variable, constraint, objective, and extraction registration in domain contexts, aggregations, model components, and pipelines. Additional business rules should enter through extra context or pipeline hooks.

## Generic Numeric Boundaries

Domain APIs should be generic over `V : RealNumber<V>` or `V : FloatingNumber<V>`. Solver adapters may convert to `Flt64`, but that conversion should not leak into reusable domain models.

## Physical Quantity Boundaries

Distance, duration, flow, demand, supply, capacity, throughput, and cost should use `Quantity<V>` or explicit dimensional wrappers. Bare numeric values should be limited to dimensionless weights, ratios, and normalized scores.

## Related Notes

- [daily.md](daily.md) records the current genericization plan.
- [ospf-kotlin-framework](../ospf-kotlin-framework/README.md) provides shared solver, pipeline, and remote-solver abstractions.

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-network-scheduling -am -DskipTests compile
```
