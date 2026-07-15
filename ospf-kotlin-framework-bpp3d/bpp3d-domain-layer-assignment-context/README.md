# BPP3D Domain — Layer Assignment Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp3d-domain-layer-assignment-context` is the layer assignment domain context for BPP3D. It implements the column-generation model for assigning items to bin layers, including variable registration, constraint pipelines, shadow price extraction, and solution extraction.

## Key Components

### Context & Aggregation

| Component | Description |
| --- | --- |
| `LayerAssignmentContext` | Top-level context for layer assignment, managing model registration and column lifecycle. |
| `ImpreciseAggregation` | Imprecise aggregation for relaxed LP column generation phases. |
| `PreciseAggregation` | Precise aggregation for final MILP solving. |
| `Bpp3dLayerAssignmentServiceAsync` | Async service facade for layer assignment operations. |

### Models (`model/`)

| Component | Description |
| --- | --- |
| `Assignment` | Assignment decision variable mapping items to bin layers. |
| `Capacity` | Capacity constraints for bins (weight, volume, depth). |
| `Load` | Load model representing the weight/volume utilization of a bin. |
| `LayerAggregation` | Aggregation of layers within a bin for model registration. |
| `Bpp3dSolverValueAdapter` | Adapter for converting between solver values (FltX) and domain quantities. |
| `ScaledBpp3dSolverValueAdapter` | Scaled variant of the solver value adapter. |
| `LayerAssignmentAliases` | Type aliases for layer assignment domain types. |

### Model Components & Pipelines

| Component | Description |
| --- | --- |
| `LayerAssignmentModelComponent` | Core model component registering assignment variables, capacity constraints, and objectives. |
| `LayerAssignmentExtraContext` | Extension point for adding custom variables, constraints, and objectives to the layer assignment model. |
| `LayerAssignmentReflux` | Reflux mechanism for column management (add/remove columns). |
| `LayerAssignmentShadowPricePipeline` | Pipeline for extracting shadow prices from the LP relaxation. |
| `LayerAssignmentSolutionExtractor` | Extracts solution (assignment decisions, KPI) from solver results. |
| `SolutionAnalyzer` | Analyzes solver solution for feasibility and quality metrics. |

### Constraint Pipelines (`service/limits/`)

| Component | Description |
| --- | --- |
| `BetterLayerMaximization` | Objective pipeline maximizing layer quality (loading rate, support). |
| `BinAmountMinimization` | Objective pipeline minimizing total bin count. |
| `BinCapacityConstraint` | Constraint pipeline for bin capacity limits. |
| `BinDepthConstraint` | Constraint pipeline for bin depth limits. |
| `BinLoadingOrderConstraint` | Constraint pipeline for loading order precedence. |
| `TailBinAssignmentConstraint` | Constraint pipeline for tail-bin assignment restrictions. |
| `TailBinLoadingRateMinimization` | Objective pipeline minimizing tail-bin loading rate. |
| `VolumeMinimization` | Objective pipeline minimizing total volume. |
| `ItemDemandConstraint` | Constraint pipeline ensuring demand fulfillment. |
| `LoadingConstraint` / `LoadingUpperBoundConstraint` | Constraint pipelines for loading limits. |
| `MaterialUsageConstraint` | Constraint pipeline for material usage constraints. |
| `PackagingConstraint` | Constraint pipeline for packaging rules. |
| `SupportAreaConstraint` | Constraint pipeline for minimum support area. |
| `WeightBalanceConstraint` / `WeightConstraint` | Constraint pipelines for weight limits. |

## Dependencies

- `bpp3d-infrastructure` — geometry, shadow price map, solver value adapter
- `bpp3d-domain-item-context` — item, material, demand models
- `ospf-kotlin-core` — MetaModel, variables, constraints, column generation

## Parent Module

[OSPF Kotlin Framework BPP3D](../README.md)
