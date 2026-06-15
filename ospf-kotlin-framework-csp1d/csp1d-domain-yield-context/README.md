# csp1d-domain-yield-context

[中文](README_ch.md)

Yield domain context for the CSP1D framework. Analyzes production yield deviation (under/over-production) and registers yield-related constraints and objectives.

## Responsibilities

- Analyze yield deviation by aggregating demand contributions per product+unit
- Detect under-production (shortfall) and over-production (surplus)
- Register yield slack variables and deviation constraints to MetaModel
- Register under/over-production penalty objectives
- Provide CGPipeline-based shadow price extraction for over-production bounds

## Key Types

| Type | Description |
|------|-------------|
| `YieldContext<V>` | Analyzes yield deviation from produce and demands |
| `YieldAggregation<V>` | Aggregation root managing yield slack variables and constraints |
| `YieldModelingConfig<V>` | Configuration for under/over-production penalties and bounds |
| `YieldAnalysis<V>` | Analysis result with under/over-production lists and outputs |
| `YieldObjectivePipeline<V>` | Objective pipeline adding under/over-production penalties |
| `YieldConstraintPipeline<V>` | CGPipeline for over-production bound constraints and shadow prices |

## Dependencies

- `fuookami.ospf.kotlin.utils` — functional utilities
- `fuookami.ospf.kotlin.math` — algebraic number types
- `fuookami.ospf.kotlin.core` — MetaModel, variables, expressions
- `fuookami.ospf.kotlin.framework` — Pipeline, CGPipeline
- `csp1d-domain-material-context` — domain entities and shadow price keys
- `csp1d-domain-produce-context` — Csp1dAggregation
