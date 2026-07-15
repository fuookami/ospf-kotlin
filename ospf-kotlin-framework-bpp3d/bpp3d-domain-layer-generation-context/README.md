# BPP3D Domain — Layer Generation Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp3d-domain-layer-generation-context` is the layer generation domain context for BPP3D. It generates candidate bin layers (columns) for the column-generation model, including circle-packing, fallback, and pile strategies.

## Key Components

| Component | Description |
| --- | --- |
| `LayerGenerationContext` | Top-level context for layer generation, managing candidate generation strategies and adapter registration. |
| `LayerGenerationProgramCandidateAdapters` | Adapters that convert generated packing candidates into layer assignment columns. |
| `Bpp3dLayerGenerationServiceAsync` | Async service facade for layer generation operations. |

## Generation Strategies

The layer generation context supports multiple candidate generation strategies:

1. **Circle Packing** — Axis-aware circle-packing grid for fixed/discrete-radius cylinders and cuboid items.
2. **Fallback** — Simple fallback layer generation for basic item combinations.
3. **Pile** — Pile-based stacking for upright `Axis3.Y` cylinders (limited support).

Generated candidates are converted to `BinLayer` columns through `LayerGenerationProgramCandidateAdapters` and registered into the column-generation model.

## Dependencies

- `bpp3d-infrastructure` — geometry primitives, circle packing
- `bpp3d-domain-item-context` — item, package models
- `bpp3d-domain-bla-context` — BLA placement algorithm
- `bpp3d-domain-block-loading-context` — block generation algorithms

## Parent Module

[OSPF Kotlin Framework BPP3D](../README.md)
