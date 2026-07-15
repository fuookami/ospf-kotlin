# BPP3D Infrastructure

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp3d-infrastructure` is the foundational infrastructure layer for the BPP3D domain framework. It provides shared geometric primitives, type aliases, and utility functions used across all domain sub-modules and the application layer.

## Key Components

### Geometry Primitives

| Component | Description |
| --- | --- |
| `Container` | Container shape definitions (2D/3D), including `Container2Shape`, `Container3Shape`, and their quantity-typed variants. Supports rest-space computation, capacity estimation, and placement feasibility checks. |
| `Cuboid` | Cuboid shape definitions with quantity-typed dimensions, volume, and weight. Includes `CuboidView` for orientation-specific dimension access. |
| `Cylinder` | Cylinder shape definitions with radius, axis, and bounding cuboid. Supports vertical (`Axis3.Y`) and horizontal (`Axis3.X`/`Axis3.Z`) cylinders. |
| `PackingShape` | Packing-oriented shape abstractions (`PackingShape2`, `PackingShape3`) that unify cuboid and cylinder geometry for placement and footprint computation. |
| `Placement` | Placement definitions (`QuantityPlacement2`, `QuantityPlacement3`, `ShapePlacement3`) with position, overlap detection, and containment checks. Supports parent-child nesting for container-in-container scenarios. |
| `Projection` | Projection definitions that map 3D cuboid views onto 2D projective planes (Bottom, Side, Front). |
| `ProjectivePlaneGeometryMapping` | Mapping between 3D geometry and 2D projective plane projections. |

### Orientation & Axis

| Component | Description |
| --- | --- |
| `Orientation` | Sealed class hierarchy for 6 cuboid orientations (Upright, UprightRotated, Side, SideRotated, Lie, LieRotated). Supports dimension permutation, category grouping, and deduplication. |
| `OrientationAxisPermutationMapping` | Mapping between orientation permutations and axis permutations for shape-aware coordinate transforms. |
| `PackageType` | Package type classification (CartonContainer, Pallet, etc.). |

### Quantity & Numeric Utilities

| Component | Description |
| --- | --- |
| `FltXAliases` | FltX-typed convenience aliases (`fltXZero`, `fltXInfinity`, `fltXEpsilon`) and conversion helpers for quantity-typed geometry. |
| `QuantityOperators` | Quantity-typed arithmetic operators (add, scale, times, ratio, ord, min, max) with FltX value extraction. |
| `QuantityContainerCore` | Quantity-typed container core definitions (`QuantityContainer2Shape`, `QuantityContainer3Shape`). |
| `QuantityGeometryCore` | Quantity-typed geometry core definitions (`QuantityPoint2/3`, `QuantityVector2/3`, `QuantityCuboid3`, etc.). |
| `SemanticParameter` | Semantic parameter definitions for packing configuration. |

### Cylinder Support & Approximation

| Component | Description |
| --- | --- |
| `ConservativeRadiusEnvelope` | Conservative radius envelope (`rMax`) for safe footprint bounding of continuous-radius cylinders. |
| `HorizontalCylinderSupportCoverage` | Horizontal cylinder support coverage checker for floor/cuboid support interval validation. |
| `PWLRadiusSquaredApproximation` | Piecewise-linear (PWL) approximation of radius-squared for continuous-radius cylinder volume. |
| `PWLRadiusApproximationConfig` | Configuration for PWL radius approximation (segments, error bounds). |

### Other

| Component | Description |
| --- | --- |
| `ShadowPriceMap` | Shadow price mapping infrastructure for column generation dual value extraction. |
| `RendererDTO` | Data transfer objects for renderer output (loading plan items, shape metadata, cylinder axis/radius/volume). |

## Dependencies

- `ospf-kotlin-utils` — functional abstractions, concepts
- `ospf-kotlin-math` — algebra, geometry
- `ospf-kotlin-quantities` — physical quantity types and units

## Parent Module

[OSPF Kotlin Framework BPP3D](../README.md)
