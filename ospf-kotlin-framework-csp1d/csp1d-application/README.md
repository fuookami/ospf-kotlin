# csp1d-application

[中文](README_ch.md)

Application layer for the CSP1D framework. Orchestrates problem definition, solving (MILP and column generation), recovery, solution enrichment, and KPI rendering.

## Responsibilities

- Define CSP1D problem and solve configuration (Csp1dProblem, Csp1dSolveConfig)
- Orchestrate plain MILP solving via Csp1dMilp
- Orchestrate column generation via Csp1dColumnGeneration (LP master, pricing loop, final MILP)
- Manage shadow price lifecycle (Csp1dShadowPriceLifecycle)
- Support warm-start recovery from previous solutions (Csp1dRecovery)
- Enrich solutions with KPI, trace, and render data
- Support Top-K cutting plan selection
- Define solution analyzer interface for custom analysis

## Key Types

| Type | Description |
|------|-------------|
| `Csp1dProblem<V>` | Problem definition with products, materials, demands, and config |
| `Csp1dSolveConfig<V>` | One-stop solve config with extensions and policies |
| `Csp1dColumnGeneration<V>` | Column generation solver with LP master, pricing, and final MILP |
| `Csp1dMilp<V>` | Plain MILP solver |
| `Csp1dMilpSolver<V>` | Low-level MILP solver with model building and solution extraction |
| `Csp1dProduceContext<V>` | Model context bridging ProduceAggregation and extensions |
| `Csp1dShadowPriceLifecycle<V>` | Shadow price refresh and extraction via CGPipeline |
| `Csp1dRecovery<V>` | Recovery solver with warm-start adapter support |
| `Csp1dSolution<V>` | Solution with produce, KPI, render, status, and trace |
| `Csp1dColumnGenerationTrace` | Column generation trace with termination reason and statistics |
| `Csp1dSolutionAnalyzer<V>` | Interface for custom solution analysis |

## Dependencies

- `fuookami.ospf.kotlin.utils` — functional utilities
- `fuookami.ospf.kotlin.math` — algebraic number types
- `fuookami.ospf.kotlin.core` — MetaModel, solver output
- `fuookami.ospf.kotlin.framework` — Pipeline, CGPipeline, ColumnGenerationSolver
- `csp1d-infrastructure` — render DTOs
- `csp1d-domain-material-context` — domain entities, shadow price keys
- `csp1d-domain-cutting-plan-generation-context` — generators, pricing
- `csp1d-domain-produce-context` — ProduceAggregation, policies, extensions
- `csp1d-domain-yield-context` — yield analysis and modeling
- `csp1d-domain-wasting-minimization-context` — waste analysis
- `csp1d-domain-length-assignment-context` — length assignment
