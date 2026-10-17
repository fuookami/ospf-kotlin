# BPP2D Domain — Layer Generation Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp2d-domain-layer-generation-context` provides Candidate layer generation with shape dispatch: block, space-split, BLA, pattern, circle grid (square / hexagonal) and mixed shape.

## Public API

`LayerGenerationContext` — composite generator with shape dispatch
`Bpp2dLayerGenerationRequest` / `Bpp2dLayerGenerationResult`
`CircleGridGenerator` — square and hexagonal circle grids
`MixedShapeLayerGenerator` — rectangle row plus circle grid, pairwise validated
`HistoricalLayerGenerator` — replays existing columns

## Parent Module

[OSPF Kotlin Framework BPP2D](../README.md)
