# Generalized Flow

[中文](README_ch.md) | [Framework overview](../README.md)

Public API: `GeneralizedFlowProblem<V>`, `GeneralizedFlowContext<V>`, and `GeneralizedFlowSolution<V>`.

Each directed service arc has one input quantity at its origin and produces an arrival quantity at its destination. The model enforces `outgoing input - incoming output = node net supply`; it deliberately does not reuse ordinary flow conservation. Arc capacities and tariffs apply to input quantities. Yield/loss applies after the independent unit conversion: `output = input * inputToOutputFactor * productionYield`. Same-dimension unit conversion is inferred; cross-dimension conversion requires an explicit ratio quantity with unit `destinationUnit / sourceUnit`.

Every arc input has a finite upper bound. This keeps loss/gain circuits bounded even when their product of yields permits unbounded circulation. Hard node balances are used; shortages, BOM recipes, multi-input transformations, MRP, and scheduling are outside this first version.

The immutable problem is separate from per-model registration state. Register a fresh context against each `AbstractLinearMetaModel<Flt64>`; failed or cross-model reuse is rejected. Domain quantities and values use `Quantity<V>` and `V : RealNumber<V>`. Solver conversion is centralized by `NetworkSchedulingSolverValueAdapter<V>`. Downstream business rules can be added through pipeline factories that receive registered input/output expressions.

`GeneralizedFlowSolution` reports, by arc, input, unit-converted output, realized output, signed loss/gain, and input tariff; by node, declared supply, outgoing input, incoming output, realized net supply, and residual. Default tests use hand-calculated witnesses and small model checks without native solver dependencies.

`GeneralizedFlowContext` accepts an optional `GeneralizedFlowSolverTolerance` for solution extraction. Its absolute and relative tolerances apply to normalized `Flt64` solver values, not domain quantities, so they must be chosen in the solver adapter's value scale. Extraction returns a structured failure for missing or non-finite values, negative inputs, inputs outside capacity bounds beyond tolerance, or node-balance residuals beyond tolerance; solver values that cannot be converted back to `V` also fail extraction.
