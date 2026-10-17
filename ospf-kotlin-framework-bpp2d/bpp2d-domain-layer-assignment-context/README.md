# BPP2D Domain — Layer Assignment Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp2d-domain-layer-assignment-context` provides Column-generation model: assignment, load, capacity, demand constraint with shadow prices and objectives. This is the only module that touches `FltX`.

## Public API

`ImpreciseAssignment` — one integer variable per column (RMP)
`PreciseAssignment` — copies per column with copy upper bounds (final MILP)
`ImpreciseLoad` / `PreciseLoad` — demand load symbols
`PreciseLoadCapacity` — loaded area and loading rate per column
`DemandConstraint` — coverage lower bound plus optional upper bound, with shadow prices
`AreaMinimization` / `SheetAmountMinimization` / `SheetCapacityConstraint`
`LayerAssignmentContext` — five-stage lifecycle

## Parent Module

[OSPF Kotlin Framework BPP2D](../README.md)
