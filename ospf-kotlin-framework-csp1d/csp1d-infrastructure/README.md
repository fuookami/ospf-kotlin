# csp1d-infrastructure

[中文](README_ch.md)

Infrastructure layer for the CSP1D framework. Contains shared enums and DTOs used across domain and application layers.

## Responsibilities

- Define rendering DTOs (`RenderCuttingPlanDTO`, `RenderSchemaDTO`) for UI serialization
- Define shared enums (`CuttingPlanProductOrder`) used by domain models

## Key Types

| Type | Description |
|------|-------------|
| `CuttingPlanProductOrder` | Cutting plan product ordering strategy (Asc, Desc, UserDefined) |
| `RenderCuttingPlanProductionDTO` | Serializable DTO for a single production entry in a cutting plan |
| `RenderCuttingPlanDTO` | Serializable DTO for a complete cutting plan rendering |
| `RenderSchemaDTO` | Top-level render schema containing KPI metrics and cutting plans |
| `RenderProductionType` | Enum distinguishing Product vs Costar entries |

## Dependencies

This module has no internal CSP1D dependencies. It depends only on `fuookami.ospf.kotlin.math` for numeric types and `kotlinx.serialization` for DTO annotations.
