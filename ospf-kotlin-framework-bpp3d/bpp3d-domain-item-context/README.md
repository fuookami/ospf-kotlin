# BPP3D Domain — Item Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp3d-domain-item-context` is the item domain context for BPP3D. It defines the core domain models for items, packages, materials, bins, layers, and packing patterns, along with their aggregation and column-generation model components.

## Key Components

### Models (`model/`)

| Component | Description |
| --- | --- |
| `Item` | Item model with demand, shape spec, and orientation constraints. |
| `Package` | Package model with shape, weight, and attribute. |
| `Material` | Material model with key, type, weight, and dimensional constraints. |
| `Bin` | Bin model representing a packing container with capacity and constraints. |
| `Layer` | Layer model for vertical layering within bins. |
| `Block` | Block model for block-loading candidate generation. |
| `Pattern` | Packing pattern representing a solution plan for a bin. |
| `Schema` | Packing schema describing item-package configuration rules. |
| `ItemContainer` | Container for managing item collections with shape and demand statistics. |
| `DemandStatistics` / `QuantityDemandStatistics` | Demand statistics (total amount, weight, volume) for items. |
| `DemandReducedCost` / `QuantityDemandReducedCost` | Demand reduced-cost model for column generation pricing. |
| `ShadowPriceMap` | Shadow price map for dual value lookup by item/material. |
| `PlacementFactory` | Factory for creating placement instances from items and orientations. |
| `PlacementPlaneMapping` | Mapping between 3D placements and 2D projective plane projections. |
| `PlacementTyping` | Type aliases for placement type variants. |
| `CylinderShapeContract` | Cylinder shape contract for axis-aware capability checks. |
| `PackageAttribute` | Package attributes (fragility, stackability, etc.). |
| `ContinuousRadiusModelComponent` | Model component for continuous-radius cylinder variable registration, PWL approximation, and result extraction. |
| `ContinuousRadiusSelectionExtractor` | Extractor for continuous-radius selection results from solver solutions. |
| `QuantityDomainModels` | Quantity-typed domain model aliases and adapters. |

### Services (`service/`)

| Component | Description |
| --- | --- |
| `Bpp3dItemServiceAsync` | Async service for item domain operations. |
| `ItemMerger` | Merges items with identical shape and orientation into grouped demands. |
| `ItemHeightCombinator` | Combines items by height for layer generation. |
| `LoadingOrderCalculator` | Calculates loading order for items in bins. |

### Aggregation

| Component | Description |
| --- | --- |
| `Aggregation` | Item aggregation combining multiple model components for model registration. |
| `ItemContext` | Top-level context exposing item domain capabilities to the application layer. |

## Dependencies

- `bpp3d-infrastructure` — geometry primitives, types
- `ospf-kotlin-core` — optimization model, variables, constraints
- `ospf-kotlin-quantities` — physical quantity types

## Parent Module

[OSPF Kotlin Framework BPP3D](../README.md)
