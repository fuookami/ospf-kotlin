# ospf-kotlin-framework-csp2d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-framework-csp2d` is the planned two-dimensional cutting stock framework module. It is currently a Maven module shell with no tracked Kotlin implementation.

## Scope

The module is reserved for reusable CSP2D domain modeling:

1. Sheet, product, cutting pattern, and cutting plan models.
2. Quantity-aware coordinates, dimensions, area, and waste representation.
3. Model components and pipelines for future MILP or column-generation registration.

Business DTO protocols, formula languages, project runtime settings, and solver plugin selection should remain in downstream adapters.

## Public API

No stable runtime public API exists yet. Future APIs should start with generic and quantity-aware domain types rather than fixed `Double` or `Flt64` models.

## Modeling Extensions

Future implementation should keep optimization model assembly in context, aggregation, model component, and pipeline types. Application services should orchestrate solving and recovery, not directly accumulate constraints and objective terms.

## Generic Numeric Boundaries

The reusable domain layer should be generic over `V : RealNumber<V>` or `V : FloatingNumber<V>`. Solver-specific `Flt64` conversions should be isolated in adapters.

## Physical Quantity Boundaries

Coordinates, cutting positions, sheet dimensions, product dimensions, used area, and waste area should use `Quantity<V>` or dedicated two-dimensional quantity types. Bare `V` should be limited to dimensionless utilization, waste ratio, penalties, and scores.

## Related Notes

- [daily.md](daily.md) records the current genericization plan.
- [ospf-kotlin-framework-csp1d](../ospf-kotlin-framework-csp1d/README.md) provides the mature 1D cutting stock reference.

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-csp2d -am -DskipTests compile
```
