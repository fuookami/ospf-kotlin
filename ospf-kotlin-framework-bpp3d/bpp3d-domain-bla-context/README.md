# BPP3D Domain — BLA Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp3d-domain-bla-context` is the Bottom-Up Left-Justified Assignment (BLA) domain context for BPP3D. It provides the BLA placement algorithm that positions items within a 2D projective plane using a bottom-up left-justified heuristic.

## Key Components

| Component | Description |
| --- | --- |
| `BLAContext` | Top-level BLA context exposing placement capabilities to the application layer. |
| `BottomUpLeftJustifiedAlgorithm` | 2D BLA algorithm that places items sequentially on a projective plane, always choosing the lowest available position, then the leftmost. |
| `BottomUpLeftJustifiedAlgorithm3D` | 3D extension of the BLA algorithm that operates on the full 3D space with depth stacking. |
| `Bpp3dBlaAsync` | Async service facade for BLA domain operations. |

## Algorithm Details

The BLA algorithm processes items in a given order:

1. For each item, find the lowest available Y coordinate on the projective plane.
2. Among positions at that Y level, find the leftmost available X coordinate.
3. Place the item at that position if it fits within the container bounds.

The 3D variant extends this to also consider Z-axis depth ordering, stacking items from bottom to top and from front to back.

## Dependencies

- `bpp3d-infrastructure` — geometry primitives, projective planes, placement types
- `bpp3d-domain-item-context` — item and package models

## Parent Module

[OSPF Kotlin Framework BPP3D](../README.md)
