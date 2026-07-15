# ospf-kotlin-framework-bpp2d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-framework-bpp2d` is the early-stage two-dimensional rectangular packing framework module. It currently contains a minimal quantity-aware domain kernel for rectangular packing demand and geometry mapping.

## Scope

This module focuses on reusable BPP2D concepts:

1. Rectangle items, sheets, placements, and packing scenes.
2. Quantity-aware projection, placement, and box needs.
3. Geometry mapping to the shared quantity geometry model without depending on BPP3D packages.

Downstream request DTOs, tenant parameters, project-specific policies, solver selection, and service orchestration belong outside this module until they become reusable domain concepts.

## Public API

Current production entry points include:

| API | Responsibility |
| --- | --- |
| `RectangleItem2<V>` | Rectangle item dimensions and rotation allowance |
| `Sheet2<V>` | Sheet dimensions |
| `Projection2Need<V>` | Width/height projection and area |
| `Placement2Need<V>` | Position plus projection |
| `Box2Need<V>` | Bounds, overlap, intersection, and inside checks |
| `PlannedRectangle2<V>` | Item placement with optional rotation |
| `PackingScene2<V>` | Sheet-level inside, area, utilization, and overlap analysis |
| `toGeometryProjection2()` / `toGeometryPlacement2()` / `toGeometryBox2()` | Stable adapters to quantity geometry |

## Modeling Extensions

The module does not yet expose a solver model or application solver. When optimization modeling is added, model registration should be placed in domain contexts, aggregations, model components, and pipelines rather than hard-coded in an application service.

## Generic Numeric Boundaries

Current domain models are generic over `V : FloatingNumber<V>`. Future solver adapters may convert values to `Flt64`, but the reusable domain layer should stay generic.

## Physical Quantity Boundaries

Widths, heights, coordinates, and areas are represented through `Quantity<V>`. Dimensionless values such as utilization are exposed as `V`.

## Related Notes

- [daily.md](daily.md) records the implementation and genericization plan.
- [ospf-kotlin-math geometry README](../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/geometry/README.md) documents the shared geometry layer.

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-bpp2d -am test
```
