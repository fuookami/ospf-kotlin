# csp1d-domain-wasting-minimization-context

[中文](README_ch.md)

Waste minimization domain context for the CSP1D framework. Analyzes and quantifies waste (rest width, rest material area) across selected cutting plans.

## Responsibilities

- Analyze rest width waste per cutting plan (scaled by batch count)
- Analyze rest material area proxy waste (rest width × material length)
- Register waste penalty objectives (trim width, rest material, material cost)

## Key Types

| Type | Description |
|------|-------------|
| `WastingMinimizationContext<V>` | Analyzes waste across selected cutting plans |
| `WasteAnalysis<V>` | Analysis result with rest width/material waste lists and totals |
| `RestWidthWaste<V>` | Rest width waste record for a cutting plan |
| `RestMaterialWaste<V>` | Rest material area proxy waste record |
| `WasteAggregation<V>` | Aggregation root for waste objective configuration |
| `WasteObjectivePipeline<V>` | Objective pipeline adding waste penalty terms |

## Dependencies

- `fuookami.ospf.kotlin.utils` — functional utilities
- `fuookami.ospf.kotlin.math` — algebraic number types
- `fuookami.ospf.kotlin.core` — MetaModel, variables
- `fuookami.ospf.kotlin.framework` — Pipeline
- `csp1d-domain-material-context` — domain entities
- `csp1d-domain-produce-context` — ProduceAggregation, Csp1dAggregation
