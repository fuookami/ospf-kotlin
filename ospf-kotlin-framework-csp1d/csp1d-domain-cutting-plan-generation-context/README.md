# csp1d-domain-cutting-plan-generation-context

[中文](README_ch.md)

Cutting plan generation domain context for the CSP1D framework. Provides enumerative and pricing-based generators for producing candidate cutting plans.

## Responsibilities

- Generate initial cutting plans via DFS, N-Sum, N-Same, and FullSum enumerators
- Generate pricing candidates via reduced-cost pricing generator
- Manage generation constraints (knife count, over-produce length, width bound)
- Provide canonical key deduplication and dominance pruning
- Support parallel generation by material with cross-worker deduplication
- Cache quantity computations, width indices, and slice templates for performance

## Cache Construction Boundaries

DFS, N-Sum and FullSum allocate slice-template recorders only when their template cache
is enabled, including material-parallel paths. Custom constraints that disable reuse no
longer record leaf templates that would be discarded. Plan budgets, deadline checks,
canonical identity and constraint callbacks retain their existing behavior.

The width-index cache still serializes cold builds, including custom width callbacks.
Template cache lookup and publication remain separate: concurrent equivalent materials
may repeat a cold search. No worker waits for another worker's complete DFS, and partial
or timed-out templates are not published. Per-key concurrency or background prewarming
requires a separate purity and deadline/cancellation contract; it is not inferred from
the absence of a custom constraint.

## Key Types

| Type | Description |
|------|-------------|
| `DFSGenerator<V>` | Stack-based depth-first search for multi-product combinations |
| `NSumGenerator<V>` | Depth-limited DFS enumerating width-sum combinations |
| `NSameGenerator<V>` | Single-product plans per product-width combination |
| `FullSumGenerator<V>` | Enumerate all width-sum combinations per material |
| `ReducedCostPricingGenerator<V>` | Pricing generator using shadow prices and reduced cost |
| `CostarFiller<V>` | Fills remaining width with costar slices |
| `GenerationConstraints<V>` | Constraint configuration (knife count, length, parallelism, dominance) |
| `CuttingPlanConstraint<V>` | Composable constraint predicate for pruning and feasibility |
| `CuttingPlanCanonicalKey` | Structural deduplication key for cutting plans |
| `GenerationCollector<V>` | Collects and filters candidates with dedup and dominance |

## Dependencies

- `fuookami.ospf.kotlin.utils` — functional utilities
- `fuookami.ospf.kotlin.math` — algebraic number types
- `fuookami.ospf.kotlin.quantities` — physical quantity types
- `csp1d-domain-material-context` — domain entities (Product, Material, CuttingPlan, etc.)
