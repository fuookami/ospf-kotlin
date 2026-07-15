# BPP3D Application

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp3d-application` is the application layer for BPP3D. It orchestrates the column-generation workflow, combining all domain contexts into a complete packing pipeline: item modeling, layer generation, layer assignment, packing, and result output.

## Key Components

| Component | Description |
| --- | --- |
| `ColumnGenerationApplicationService` | Top-level application service entry point. Accepts packing input, configures the column-generation algorithm, and returns packing results. |
| `ColumnGenerationAlgorithm` | Column-generation algorithm orchestrator. Manages the iterative LP → pricing → column addition → final MILP workflow. |
| `ColumnGenerationPackingAnalyzer` | Analyzes column-generation results, computes KPI (loading rate, utilization), and produces the final packing plan. |
| `ColumnGenerationStandardExecutors` | Standard executor implementations for column-generation lifecycle hooks (pricing, column management, finalization). |
| `DepthBoundaryLayerOrientationPolicy` | Application-level policy constraining the first and last depth layers' allowed cylinder axes and cuboid orientations. Applied as hard validation after final MILP solving. |
| `LayerPlacementAdapter` | Adapter converting generated layer candidates into concrete 3D placements within bins. |

## Workflow

```
Input Items → ItemContext → LayerGenerationContext → LayerAssignmentContext → PackingContext → Output
                                │                         │                      │
                          Generate columns        Column generation LP      Material packing
                          (BLA, block loading)     Shadow price extraction   Geometry validation
                                                                           Renderer output
```

1. **Item Modeling** — `ItemContext` registers items, packages, and materials.
2. **Layer Generation** — `LayerGenerationContext` generates candidate bin layers via BLA, circle packing, or block loading.
3. **Layer Assignment** — `LayerAssignmentContext` runs column generation to optimally assign items to layers.
4. **Packing** — `PackingContext` finalizes material packing with geometry validation.
5. **Output** — Results are exported as renderer DTOs and KPI reports.

## Dependencies

- `bpp3d-infrastructure` — geometry primitives, renderer DTO
- `bpp3d-domain-item-context` — item domain
- `bpp3d-domain-bla-context` — BLA placement
- `bpp3d-domain-block-loading-context` — block loading
- `bpp3d-domain-layer-assignment-context` — layer assignment
- `bpp3d-domain-layer-generation-context` — layer generation
- `bpp3d-domain-packing-context` — packing

## Parent Module

[OSPF Kotlin Framework BPP3D](../README.md)
