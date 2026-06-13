# BPP3D Domain — Packing Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp3d-domain-packing-context` is the packing domain context for BPP3D. It manages the final packing process, including material packing, geometry validation, renderer output, and solution assembly.

## Key Components

### Context & Aggregation

| Component | Description |
| --- | --- |
| `PackingContext` | Top-level context for packing, managing material packing execution and result collection. |
| `Aggregation` | Packing aggregation combining material packing plans and KPI. |

### Models (`model/`)

| Component | Description |
| --- | --- |
| `MaterialAttribute` | Material attributes (dimensions, weight, shape) for packing input. |
| `MaterialPackingNumbers` | Material packing number aliases for quantity tracking. |
| `MaterialPackingPlan` | Material packing plan representing per-material packing decisions. |
| `PackageSolutionLikeAdapter` | Adapter for converting packing solutions into a solution-like interface for downstream consumption. |

### Services (`service/`)

| Component | Description |
| --- | --- |
| `Packer` | Core packing service that orchestrates material-to-bin assignment and coordinate finalization. |
| `MaterialPacker` | Specialized packer for single-material packing scenarios. |
| `MaterialPackingSolverExecutor` | Solver execution strategy for material packing. |
| `ExhaustiveMaterialPackingSolverExecutor` | Exhaustive search strategy that tries all material packing combinations. |
| `PackingGeometryContract` | Geometry contract defining capability paths for cuboid and cylinder packing. |
| `PackingGeometryGuard` | Guard that validates packing geometry (overlap, containment, support) against real cylinder shape. |
| `PackingRendererAdapter` | Adapter that converts packing results into renderer DTO output. |

## Dependencies

- `bpp3d-infrastructure` — geometry primitives, renderer DTO, support coverage
- `bpp3d-domain-item-context` — item, package, material models
- `bpp3d-domain-layer-assignment-context` — layer assignment results
- `bpp3d-domain-layer-generation-context` — layer generation results
- `bpp3d-domain-bla-context` — BLA placement algorithm

## Parent Module

[OSPF Kotlin Framework BPP3D](../README.md)
