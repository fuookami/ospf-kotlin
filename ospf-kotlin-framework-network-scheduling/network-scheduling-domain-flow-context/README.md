# Flow

[简体中文](README_ch.md) | [Framework guide](../README.md)

Public API: `FlowProblem<V> / FlowContext<V> / FlowAggregation<V>`.

Commodity units and explicit capacity usage; hard balances unless bounded slack is configured.

Use the context to register into an existing `AbstractLinearMetaModel<Flt64>`, or the corresponding Network application service for standalone solving. Domain values use `V : RealNumber<V>` and `Quantity<V>`; conversion stays at the solver boundary. A context/aggregation belongs to one model; create a fresh context from the same immutable problem for another model. Registration failures poison that context rather than retrying a partially modified model.

Downstream code can inspect registered expressions and add constraints/objectives through standard pipelines. Default tests use independent witnesses and small enumeration oracles, not a native solver. See the framework guide for scenarios, units and proof boundaries.
