# csp1d-domain-length-assignment-context

[中文](README_ch.md)

Length assignment domain context for the CSP1D framework. Handles dynamic coil length derivation and over-length detection for dynamic-length products.

## Responsibilities

- Derive coil length from demand quantity and product properties via injected LengthDerivation
- Detect over-length violations against maxOverProduceLength
- Register length assignment variables, constraints, and objectives to MetaModel
- Support lower/upper bound constraints and over-length slack variables

## Key Types

| Type | Description |
|------|-------------|
| `LengthAssignmentContext<V>` | Executes length assignment with injected derivation function |
| `LengthDerivation<V>` | Functional interface for deriving coil length from demand |
| `LengthAssignmentInput<V>` | Input with dynamic products, demands, and constraints |
| `LengthAssignmentResult<V>` | Result with assignments and over-length records |
| `LengthAssignment<V>` | Single product's assigned length and batch count |
| `LengthAssignmentModelingConfig<V>` | Configuration for bounds, penalties, and batch minimization |
| `LengthAggregation<V>` | Aggregation root managing length variables and constraints |
| `LengthConstraintPipeline<V>` | Constraint pipeline for length bounds |
| `LengthObjectivePipeline<V>` | Objective pipeline for length-related penalties |

## Dependencies

- `fuookami.ospf.kotlin.utils` — functional utilities
- `fuookami.ospf.kotlin.math` — algebraic number types
- `fuookami.ospf.kotlin.core` — MetaModel, variables
- `fuookami.ospf.kotlin.framework` — Pipeline
- `csp1d-domain-material-context` — domain entities
- `csp1d-domain-produce-context` — Csp1dAggregation, ProduceAggregation
