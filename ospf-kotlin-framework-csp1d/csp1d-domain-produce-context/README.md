# csp1d-domain-produce-context

[中文](README_ch.md)

Produce domain context for the CSP1D framework. Manages the master problem variables, intermediate symbols, and constraint/objective registration for cutting plan selection.

## Responsibilities

- Manage x variables (plan usage counts) and batch intermediate symbols
- Register demand, material, machine batch, and machine capacity intermediate symbols
- Support column generation incremental addColumns with flush + asMutable
- Define modeling extension point interfaces (Pipeline, CGPipeline, IncrementalPipeline)
- Define policy interfaces (objective, generation, pricing, flow, extraction)
- Define extension set container for all policy categories

## Key Types

| Type | Description |
|------|-------------|
| `ProduceAggregation<V>` | Aggregation root managing x variables and constraint intermediate symbols |
| `Csp1dAggregation<V>` | Interface for aggregation roots registering to MetaModel |
| `Csp1dModelContext<V>` | Interface for model registration and solution extraction |
| `Csp1dIterativeContext<V>` | Column generation context with addColumns and shadow price extraction |
| `Csp1dModelingExtension<V>` | Carries additional pipelines for injection at solve stages |
| `Csp1dFlowPolicy<V>` | Flow control policy (initial plan filter, equivalence, termination, partial) |
| `Csp1dExtractionPolicy<V>` | Solution enrichment policy (custom KPI details) |
| `Csp1dExtensionSet<V>` | Unified container for all extension policies |

## Dependencies

- `fuookami.ospf.kotlin.utils` — functional utilities
- `fuookami.ospf.kotlin.math` — algebraic number types
- `fuookami.ospf.kotlin.core` — MetaModel, variables, expressions
- `fuookami.ospf.kotlin.framework` — Pipeline, CGPipeline
- `csp1d-domain-material-context` — domain entities
- `csp1d-domain-cutting-plan-generation-context` — canonical key
