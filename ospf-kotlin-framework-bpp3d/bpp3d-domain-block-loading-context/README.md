# BPP3D Domain — Block Loading Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp3d-domain-block-loading-context` is the block loading domain context for BPP3D. It provides block generation algorithms and search strategies for constructing packing candidates from item combinations.

## Key Components

### Context & Aggregation

| Component | Description |
| --- | --- |
| `BlockLoadingContext` | Top-level block loading context exposing candidate generation capabilities. |
| `Bpp3dBlockLoadingAsync` | Async service facade for block loading operations. |

### Models (`model/`)

| Component | Description |
| --- | --- |
| `Space` | Spatial region model used by search algorithms for recursive space splitting. |

### Block Generators (`service/`)

| Component | Description |
| --- | --- |
| `SimpleBlockGenerator` | Generates simple blocks from single-item vertical stacking (only for`Axis3.Y` cylinders). |
| `ComplexBlockGenerator` | Generates complex blocks by combining multiple items into heterogeneous stacks with support coverage checks. |

### Search Algorithms (`service/`)

| Component | Description |
| --- | --- |
| `DepthFirstSearchAlgorithm` | DFS-based space splitting algorithm for cuboid-only packing paths. |
| `MultiLayerHeuristicSearchAlgorithm` | Multi-layer heuristic search that combines layer-by-layer stacking with horizontal cylinder support validation. |
| `CylinderUnsupportedGuard` | Guard that rejects unsupported horizontal cylinders in generated candidate blocks. |

## Dependencies

- `bpp3d-infrastructure` — geometry primitives, support coverage
- `bpp3d-domain-item-context` — item, package, and layer models

## Parent Module

[OSPF Kotlin Framework BPP3D](../README.md)
