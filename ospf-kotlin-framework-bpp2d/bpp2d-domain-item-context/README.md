# BPP2D Domain — Item Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp2d-domain-item-context` provides Item, sheet, layer, packing program, demand statistics, solver value adapter and the legacy `*Need` compatibility bridge.

## Public API

`ActualItem` / `PatternItem` / `AbstractItem` — items over a packing shape
`SheetType` / `Sheet` / `Layer` / `SheetLayer` — container hierarchy
`PackingProgram` — repeatable layout program
`Bpp2dAssignmentDemandEntry`-adjacent demand statistics types
`Bpp2dSolverValueAdapter` — solver value conversion boundary
`LegacyBridge` — `*Need` to new-model adapters

## Parent Module

[OSPF Kotlin Framework BPP2D](../README.md)
