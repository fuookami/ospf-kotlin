# ospf-kotlin-framework-csp2d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-framework-csp2d` is the two-dimensional cutting stock framework of OSPF Kotlin. It follows the CSP
semantics of [ospf-kotlin-framework-csp1d](../ospf-kotlin-framework-csp1d/README.md) (products, demands, cutting
patterns, cutting plans, yield, waste, sheet assignment, column generation) while adopting the generic layering
discipline of [ospf-kotlin-framework-bpp2d](../ospf-kotlin-framework-bpp2d/README.md): the domain layer is generic
over `V`, and solver-specific numeric types appear only at the modelling boundary and in the application layer.

The 2D geometry is shared with bpp2d through
[ospf-kotlin-framework-geometry2d](../ospf-kotlin-framework-geometry2d/README.md).

## Scope

Two supply modes, one model:

| Mode | Problem class | Material model | Decision variables |
| --- | --- | --- | --- |
| Coil cutting | Strip packing (fixed width, length is a decision) | `width` fixed, `length = null` | consumed length and layout |
| Fixed-size sheets | 2D bin packing | `width × height` fixed | sheet count and layout |

Cutting processes are configurable and orthogonal to the mode:

| Process | Meaning | Equipment |
| --- | --- | --- |
| `Freeform` | Arbitrary nesting, only placement coordinates are needed | laser, waterjet, CNC |
| `GuillotineTwoStage` | Strips first, then pieces; a cut tree is produced | shear, panel saw |
| `GuillotineThreeStage` | Strips, then blocks, then pieces; a cut tree is produced | shear, panel saw |

Irregular (non-rectangular) parts are explicitly out of scope.

## Module Structure

| Module | Description |
| --- | --- |
| `csp2d-infrastructure` | Cutting regions, kerf, guillotine cut lines and cut tree, cutting process, errors and render DTOs |
| `csp2d-domain-material-context` | Products, demands, materials, machines, by-products, cutting patterns and cutting plans |
| `csp2d-domain-pattern-generation-context` | Cutting pattern generators (freeform and guillotine) and reduced-cost pricing |
| `csp2d-domain-produce-context` | Master problem variables, demand and capacity pipelines, modelling extensions |
| `csp2d-domain-yield-context` | Under- and over-production analysis, yield constraints and objectives |
| `csp2d-domain-wasting-minimization-context` | Remnant area analysis and waste penalty objectives |
| `csp2d-domain-sheet-assignment-context` | Consumed length derivation for coils, sheet count and size selection for fixed sheets |
| `csp2d-application` | Problem definition, MILP and column generation solving, recovery, result enrichment |

## Current Status

Phase C2-0 is complete: the eight-module skeleton exists and `csp2d-infrastructure` provides the cutting region model,
the kerf-aware region split, the guillotine cut tree with k-stage validation, the cutting process configuration, the
error types and the renderer DTOs. Later phases add the material and pattern models, pattern generation, the
modelling contexts, the application layer and the dataset/Gurobi integration.

## Cut Tree Semantics

A guillotine plan is not a set of coordinates but a **cut tree**: internal nodes are through cuts and leaves are the
final pieces. The stage of a cut is its level: the root cut is level 1, a cut below it must be exactly level 2, and so
on. A two-stage process therefore means exactly "strips then pieces" and a three-stage process means "strips, blocks,
pieces". `CutTree2.validate` rejects trees that violate the region split (including the kerf strip), exceed the process
stage limit, or place a child cut at the wrong level.

## Generic Numeric Boundaries

1. The domain layer is generic over `V : FloatingNumber<V>`.
2. Coordinates, dimensions and areas are `Quantity<V>`; dimensionless values are bare `V`.
3. Solver numeric types appear only at the modelling boundary and in the application layer.
4. Generic code must not call `toDouble()`.

## Related Notes

- [daily.md](daily.md) records the implementation plan and progress.
- [ospf-kotlin-framework-csp1d](../ospf-kotlin-framework-csp1d/README.md) is the mature 1D cutting stock reference.
- [ospf-kotlin-framework-geometry2d](../ospf-kotlin-framework-geometry2d/README.md) provides the shared 2D geometry.

## Local Validation

```powershell
mvn -B -ntp -f ospf-kotlin-framework-csp2d/pom.xml test
```
