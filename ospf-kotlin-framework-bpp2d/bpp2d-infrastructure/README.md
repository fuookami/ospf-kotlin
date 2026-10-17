# BPP2D Infrastructure

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp2d-infrastructure` provides Quantity geometry, packing shapes (rectangle / circle), sheet shape, placement, interior-overlap tests, renderer DTOs and domain errors shared by every sub-module.

## Public API

`PackingShape2` / `RectanglePackingShape2` / `CirclePackingShape2` — shape abstraction with actual and bounding area
`Sheet2Shape` / `Sheet2Geometry` — sheet geometry and fit checks
`LayerPlacement2` — placement with overlap, containment and translation
`Box2Geometry` — interior-overlap tests for rectangle, circle and mixed pairs
`quantityPlusSafe` / `quantityGreater` / `countFitting` / `scaledQuantity` — generic quantity arithmetic
`RendererDTO` — 2D renderer DTOs
`Bpp2dValidationError` / `Bpp2dSolvingError` / `Bpp2dCapabilityError` — domain errors

## Parent Module

[OSPF Kotlin Framework BPP2D](../README.md)
