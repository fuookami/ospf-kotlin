# Network Design

[简体中文](README_ch.md) | [Framework guide](../README.md)

Public API: `NetworkDesignProblem<V> / NetworkDesignContext<V>`.

Facilities, services, discrete total capacity levels and budgets share flow registration. Levels fit the declared arc capacity envelope.

Commodity flows gated by facility or service activation require positive arc-capacity consumption so a finite linking bound can be derived. Zero consumption is rejected instead of allowing flow through closed assets or introducing an arbitrary big M.

Use the context to register into an existing `AbstractLinearMetaModel<Flt64>`, or the corresponding Network application service for standalone solving. Domain values use `V : RealNumber<V>` and `Quantity<V>`; conversion stays at the solver boundary. A context/aggregation belongs to one model; create a fresh context from the same immutable problem for another model. Registration failures poison that context rather than retrying a partially modified model.

Downstream code can inspect registered expressions and add constraints/objectives through standard pipelines. Default tests use independent witnesses and small enumeration oracles, not a native solver. See the framework guide for scenarios, units and proof boundaries.
