# Relational Query Plan

:us: English | :cn: [Simplified Chinese](README_ch.md)

`RelationalQueryPlan` is the framework-level, database-independent contract for controlled relational queries. It contains query sources, joins, predicates, projections, grouping, ordering, pagination, and optional root keys. It does not contain business dataset types, physical table names, SQL strings, permissions, or budgets.

## Core Semantics

- `QuerySource` names an adapter-registered source and may carry an alias.
- `ColumnRef` is always source-qualified at the plan boundary.
- `Inner` and `Left` compile to relational joins. `Exists` compiles to a correlated semi-join and preserves root cardinality for one-to-many predicates. Join conditions must correlate the current source with an already-bound source through column references; constant conditions are rejected, and `Exists` accepts only one-to-many or many-to-many cardinalities.
- `distinct` applies to the selected projection. `rootKey` is an explicit opt-in for root-granularity counting; adapters reject unsupported composite-key count strategies.
- `PageSpec` is applied after predicate, grouping, and ordering. Null ordering is represented explicitly by `NullsOrder`.

The plan container defensively copies its outer collections, expression containers, and known mutable payload containers at construction time. `List`, `Map`, `Set`, and array payloads are recursively snapshotted. An opaque custom payload or cyclic container that the framework cannot copy safely is retained only for diagnostics and makes `validate()` return a structured failure; it is never replaced with a fake snapshot. `canonical()`/`canonicalHash()` encode normalized expression shape and type/container structure, while deliberately omitting scalar and custom payload literal values. Plans are validated before backend compilation. Backends must keep source and column mappings in an allowlist and return structured failures for unknown sources, unknown or ambiguous columns, invalid join conditions, unsupported dialect features, and parameter binding errors. Implicit projections must use an adapter-defined registered allowlist rather than all physical table columns. A root-granularity count key must belong to the root source.

See the Ktorm plugin documentation for the SQL adapter and audit summary contract.

`SqlRelationalQueryCompiler` is a driver-free implementation that produces a parameterized
`CompiledRelationalQuery`. It quotes identifiers for each dialect and binds scalar expression values as `?`
parameters. MySQL pagination uses `LIMIT offset, limit`; PostgreSQL uses `LIMIT limit OFFSET offset`; Oracle uses
`OFFSET offset ROWS FETCH NEXT limit ROWS ONLY`. A `RelationalQuerySourceRegistry` can map logical source and field
names to an adapter-owned table and column allowlist. Without a registry, explicitly projected logical names are
used as physical names for simple adapters.

`RelationalQueryExecutionStatsRecorder` is a thread-safe in-memory audit sink. `recordSuccess` and `recordFailure`
retain the plan hash, canonical representation, duration, row count, and failure state; `snapshot()` returns the
current records without exposing the queue.

The legacy `ExpressionRepository` API remains unchanged because its synchronous list/count/update methods are not
`Ret`-based. An adapter may construct a `RelationalQueryPlan` from its existing `BooleanExpression` DSL and translate
the compiler's `Ret` result at that infrastructure boundary.
