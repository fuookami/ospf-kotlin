# csp1d-domain-material-context

[中文](README_ch.md)

Material domain context for the CSP1D framework. Defines the core domain model for products, materials, machines, cutting plans, demands, and domain policies.

## Responsibilities

- Define product, material, machine, and costar domain entities
- Define cutting plan model (slices, demand contributions, used/rest width)
- Define product demand model with discrete/continuous quantity support
- Define domain calculation context and policy interfaces for downstream extensibility
- Define shadow price key hierarchy and lightweight shadow price map
- Provide quantity arithmetic abstraction and solver value conversion utilities
- Provide render mappers for UI serialization

## Key Types

| Type | Description |
|------|-------------|
| `Product<V>` | Product with width, length, unit weight, and dynamic-length support |
| `Material<V>` | Cutting material with width range, machine binding, and batch capacity |
| `Machine<V>` | Slitting machine with batch limits, width range, and capacity |
| `Costar<V>` | Byproduct that fills remaining width (no demand contribution) |
| `CuttingPlan<V>` | Cutting plan with slices, demand contributions, and used/rest width |
| `ProductDemand<V>` | Product demand with quantity and mode label (Roll/Weight/Sheet) |
| `Csp1dDomainPolicy<V>` | Domain policy interface for width feasibility and plan judgment |
| `Csp1dShadowPriceKey` | Shadow price key hierarchy (demand, material, machine, yield) |
| `ShadowPriceMap<V>` | Lightweight shadow price container for pricing |
| `QuantityArithmetic<V>` | Quantity add/subtract abstraction over domain value types |

## Dependencies

- `fuookami.ospf.kotlin.utils` — functional utilities
- `fuookami.ospf.kotlin.math` — algebraic number types
- `fuookami.ospf.kotlin.quantities` — physical quantity types
- `fuookami.ospf.kotlin.framework` — shadow price framework
- `csp1d-infrastructure` — render DTOs
