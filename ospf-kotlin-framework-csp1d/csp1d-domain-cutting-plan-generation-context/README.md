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
