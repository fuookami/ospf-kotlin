# ospf-kotlin-framework-bpp1d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-framework-bpp1d` is the planned one-dimensional bin packing framework module. At the moment it is a Maven module shell with no tracked Kotlin implementation. Its README records the intended boundary so future implementation starts with the same architecture and documentation shape as the other framework modules.

## Scope

The module is reserved for reusable BPP1D domain modeling, such as item/bin definitions, packing policies, candidate generation, model registration, and solution extraction.

Out of scope for this module:

1. Business request DTOs and tenant-specific runtime parameters.
2. Solver backend implementations.
3. Starter dependency aggregation.

## Public API

No stable public runtime API is available yet. Future APIs should expose generic domain models and contexts rather than solver-specific `Flt64` models.

## Modeling Extensions

Future implementation should follow the framework architecture rules:

1. Keep `MetaModel` registration inside domain context, aggregation, model component, and pipeline types.
2. Put solver orchestration in the application layer only after a reusable domain kernel exists.
3. Provide extension points for extra constraints, objectives, shadow price extraction, and solution enrichment.

## Generic Numeric Boundaries

The primary domain API should be generic over `V : RealNumber<V>` or `V : FloatingNumber<V>`. Solver adapters may specialize to `Flt64`, but that conversion should stay at the adapter or registration boundary.

## Physical Quantity Boundaries

Lengths, widths, weights, capacities, and other dimensional values should use `Quantity<V>` or a module-level dimensional wrapper. Bare numeric values should be reserved for dimensionless scores, ratios, and coefficients.

## Related Notes

- [daily.md](daily.md) records the current genericization plan.
- [ospf-kotlin-framework](../ospf-kotlin-framework/README.md) provides shared solver and pipeline abstractions.

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-bpp1d -am -DskipTests compile
```
