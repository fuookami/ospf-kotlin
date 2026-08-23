# Constraint Programming Model

English | [简体中文](README_ch.md)

## Overview

The `constraint_programming` package is the solver-neutral model layer for integer constraint
programming (CP). It provides exact `Int64` domains and expressions, Boolean literals, global and
scheduling constraints, a standalone model registry, immutable snapshots, and a versioned JSON
codec.

`ConstraintProgrammingModel` is intentionally independent of the linear and quadratic
`MetaModel` hierarchy. A CP model is compiled by a `ConstraintProgrammingSolver`; it is not first
flattened into a polynomial model unless the selected solver explicitly provides an exact lowering.

```text
OSPF integer variables
  -> IntegerDomain + ConstraintProgrammingExpression + BooleanLiteral
  -> ConstraintProgrammingConstraint + IntervalVariable
  -> ConstraintProgrammingModel
  -> ConstraintProgrammingModelSnapshot
  -> backend compiler or exact lowerer
```

This package does not depend on OR-Tools. Solver adapters, including SCIP and MIP-backed adapters,
are separate from the model AST.

## Quick Start

The following model assigns three integer variables distinct values from `0..2` and minimizes the
first value. Factories and registry methods return `Ret`; propagate every failure and close a model
that is not handed to a solver.

```kotlin
import fuookami.ospf.kotlin.core.model.constraint_programming.*
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.utils.functional.*

fun buildModel(): Ret<ConstraintProgrammingModel> {
    val model = ConstraintProgrammingModel(name = "all-different")
    var completed = false
    try {
        val domain = when (val result = IntegerDomain.interval(0, 2)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val variables = List(3) { index -> IntVar("value-$index") }
        for (variable in variables) {
            when (val result = model.registerVariable(variable, domain)) {
                is Ok -> Unit
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        val expressions = variables.map { variable ->
            ConstraintProgrammingExpression.Variable(variable, domain)
        }
        val constraint = when (
            val result = ConstraintProgrammingConstraint.allDifferent(expressions)
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (
            val result = model.addConstraint(
                constraint = constraint,
                id = ConstraintId("values-all-different"),
                name = "values all different"
            )
        ) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (
            val result = model.minimize(
                expression = expressions.first(),
                id = ObjectiveId("minimize-first"),
                name = "minimize first value"
            )
        ) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = model.validate()) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        completed = true
        return ok(model)
    } finally {
        if (!completed) {
            model.close()
        }
    }
}
```

Pass the returned model to a `ConstraintProgrammingSolver` and close it after solving. Use
`snapshot()` when the model must be compiled repeatedly, serialized, or sent to a remote solver.

## Core Types

| Type | Purpose |
| --- | --- |
| [`IntegerDomain`](IntegerDomain.kt) | Contiguous or sparse exact integer domains |
| [`ConstraintProgrammingExpression`](ConstraintProgrammingExpression.kt) | Constants, integer-variable references, and integer-linear expressions |
| [`BooleanLiteral`](BooleanLiteral.kt) | Positive/negated binary-variable literals and Boolean constants |
| [`ConstraintProgrammingConstraint`](ConstraintProgrammingConstraint.kt) | Solver-neutral constraint AST and assignment evaluator |
| [`IntervalVariable`](IntervalVariable.kt) | Fixed or variable-duration, mandatory or optional scheduling interval |
| [`ConstraintProgrammingModel`](ConstraintProgrammingModel.kt) | Ordered registry and lifecycle boundary |
| [`ConstraintProgrammingModelSnapshot`](ConstraintProgrammingModelSnapshot.kt) | Repeatably compilable frozen model view |
| [`ConstraintProgrammingSnapshotCodec`](ConstraintProgrammingSnapshotCodec.kt) | Strict, versioned JSON transport codec |
| [`ConstraintGroupRegistry`](ConstraintGroupRegistry.kt) | Constraint-group integration boundary used by framework pipelines |

## Integer Domains

CP values use exact signed 64-bit integers. Floating-point values are not accepted implicitly;
scale business values to integers explicitly and check the resulting range.

Use the validated factories for model-building input:

```kotlin
val contiguous = IntegerDomain.interval(-5, 10)
val sparse = IntegerDomain.values(listOf(-3, 0, 4, 4)) // sorted and de-duplicated
val fixed = IntegerDomain.singleton(7)
val binary = IntegerDomain.boolean                    // {0, 1}
```

`IntegerDomain.enumerate()` returns `null` when cardinality is unknown or exceeds its configured
limit. This protects fake solvers and exact lowerers from accidental domain expansion.

When no domain is supplied, binary variables default to `{0, 1}`, unsigned integer variables to
`0..Long.MAX_VALUE`, and signed integer variables to the full `Long` range. These defaults are
semantically valid but usually too loose for lowering and enumeration. Prefer tight, explicit
domains.

## Expressions And Literals

`ConstraintProgrammingExpression` supports only:

- `Constant`: an `Int64` constant.
- `Variable`: an OSPF integer-variable reference plus its CP domain.
- `Linear`: integer coefficients and an integer constant.
- `Invalid`: a deferred construction error, normally produced by overflowing negation.

Use `variable`, `term`, `linear`, and `sum` for validated construction. Expression `+` and `-`
return `Ret<Linear>` because coefficient or constant normalization can overflow. `evaluate()` also
returns a structured error for missing assignments; variable-reference evaluation additionally
checks its declared domain, while linear evaluation uses checked accumulation.

`BooleanLiteral` accepts only `BinVariable`, a negation flag, or a Boolean constant:

```kotlin
val selected = binaryVariable.literal
val notSelected = binaryVariable.negatedLiteral
val alsoNotSelected = !selected
val fixedTrue = BooleanLiteral.True
```

Boolean assignments are represented as `0` and `1` in CP solution maps. Any other value is an
evaluation error.

## Constraint Families

Prefer the validated companion factories instead of constructing constraint data classes directly.

| Family | Types and semantics |
| --- | --- |
| Integer comparison | `IntegerComparison`; `equal`, `lessOrEqual`, `greaterOrEqual`, or `eq`/`leq`/`geq` with an `Int64` rhs |
| Boolean logic | `Literal`, `BoolAnd`, `BoolOr`, and `BoolXor`; XOR means exactly one true literal |
| Conditional logic | `Implication` and `Reified` with `Implies`, `ImpliedBy`, or `Equivalent` direction |
| Distinctness | `AllDifferent` over one or more integer expressions |
| Indexed selection | `Element`; the index is zero-based and the selected entry may be an integer or expression |
| Tables | `AllowedAssignments` and `ForbiddenAssignments`; every tuple must match expression arity |
| Scheduling | `NoOverlap` and `Cumulative` over registered intervals |
| Routing | `Circuit`; successors must form one permutation cycle visiting every index from `0` |
| Sequences | `Automaton`; transitions must be deterministic for each `(fromState, value)` pair |
| Inventory/resource level | `Reservoir`; events are applied in non-decreasing time order and the level must remain within bounds |

Every constraint exposes its referenced `VariableId` set and an `isSatisfied()` oracle. The oracle
is used by fake solving, remote result validation, and differential tests; backend compilation must
preserve the same semantics.

Empty Boolean collections have mathematical constant semantics: `BoolAnd` is true, while `BoolOr`
and exactly-one `BoolXor` are false. Other factories, such as `AllDifferent`, `NoOverlap`,
`Cumulative`, `Circuit`, `Automaton`, and `Reservoir`, reject empty required input.

## Intervals And Scheduling

An `IntervalVariable` contains `start`, `size`, `end`, and an optional presence literal. A present
interval must satisfy `end = start + size` and have a non-negative size. `size` may be a constant or
an integer expression, so the AST can represent both fixed and variable duration.

Scheduling uses half-open intervals `[start, end)`. Adjacent intervals do not overlap, and a
zero-duration interval occupies no time. When an optional interval is absent, evaluation returns the
canonical value `(start=0, size=0, end=0, present=false)` and scheduling constraints ignore it.

Use:

- `IntervalVariable.fixed(...)` for validated fixed duration.
- `IntervalVariable.create(...)` for expression-based duration.
- `NoOverlap.create(...)` to prevent pairwise overlap.
- `Cumulative.create(...)` for non-negative demands under a non-negative capacity.

Register each interval with `model.registerInterval(interval)`. All scalar variables referenced by
its expressions and presence literal must also be registered in the model.

AST support does not imply that every backend supports every interval form. In particular, optional
and variable-duration compilation is capability-dependent.

## Model Lifecycle

`ConstraintProgrammingModel` preserves insertion order and rejects duplicate variable, interval,
expression, constraint, and objective identities. Its normal lifecycle is:

1. Register scalar variables and their domains.
2. Register intervals and optional named expressions.
3. Add constraints, preferably with explicit IDs.
4. Add minimization or maximization objectives, preferably with explicit IDs.
5. Call `validate()` or `snapshot()`.
6. Solve or serialize the model.
7. Call `close()` to release registries; a closed model cannot be reused.

The model implements `ConstraintGroupRegistry`. Calling `registerConstraintGroup()` establishes the
current group for later constraints unless an explicit group is passed to `addConstraint()`.

`snapshot()` first verifies that every referenced variable is registered. It then copies variables,
intervals, named expressions, constraints, objectives, and group names in registration order. The
result contains no solver or native handle and may be compiled repeatedly.

## Snapshot Transport

`ConstraintProgrammingSnapshotCodec` encodes a snapshot as schema-versioned JSON. Decoding requires
a caller-provided `Map<VariableId, AbstractVariableItem<*, *>>`; transport never serializes the
sender's variable objects.

```kotlin
val encoded: Ret<String> = ConstraintProgrammingSnapshotCodec.encode(snapshot)
val decoded: Ret<ConstraintProgrammingModelSnapshot> =
    ConstraintProgrammingSnapshotCodec.decode(encodedJson, variableBindings)
```

The current codec is strict: unknown keys, unsupported schema versions, missing variable bindings,
invalid domains, malformed constraints, and non-deterministic automaton transitions return
structured errors. A snapshot payload is a portable model description, not a native solver
checkpoint.

`diagnosticActivations()` derives stable activation entries for original constraints, variable
bounds, and non-Boolean sparse domains. Solver-generated auxiliary elements must not leak into this
public evidence list.

## Identity Boundary

Use explicit `ConstraintId`, `ObjectiveId`, and `IntervalId` values whenever a model will be rebuilt,
serialized, diagnosed, or used in Benders decomposition. Default constraint IDs (`constraint-N`)
and objective IDs (`objective-N`) depend on registration order.

At the current API boundary, expression and Boolean-literal variable IDs are derived from the OSPF
variable identity as `${identifier}:${index}`. The explicit `registerVariable(id, variable, domain)`
overload changes the registry key but does not rewrite IDs inside expressions. The registry ID,
expression ID, and decode binding key must therefore remain consistent; otherwise `validate()` or
`decode()` fails. Do not treat solver row/column indices or display names as cross-rebuild identity.

## Solver Capabilities

The model AST is broader than any single backend. Query the selected solver descriptor before
relying on a feature:

```kotlin
val support = solver.descriptor.capabilities.constraintProgrammingSupport(
    ConstraintProgrammingFeature.OptionalInterval
)
```

Support levels are:

- `Native`: compiled to a backend-native constraint or handled directly by that solver.
- `ExactLowering`: transformed to a proven equivalent formulation.
- `Unsupported`: the solver must reject the feature with a structured error.

The fake solver is a deterministic contract/testing implementation with enumeration limits, not a
production search engine. SCIP and MIP-backed adapters each support declared subsets; inspect their
runtime descriptors rather than inferring support from the existence of an AST class. Exact MIP
lowering also requires safe finite bounds and may reject models that exceed configured size gates.

## Error Handling

Model-building operations use `Ret<T>` or `Try`; invalid input is not reported through business
exceptions. Prefer these validated entry points:

- `IntegerDomain.interval`, `IntegerDomain.values`, and `IntegerDomain.sparse`.
- `ConstraintProgrammingExpression.variable`, `term`, `linear`, and `sum`.
- `ConstraintProgrammingConstraint` companion factories.
- `IntervalVariable.create`/`fixed`, `NoOverlap.create`, and `Cumulative.create`.
- `ConstraintProgrammingModel.validate` and `snapshot` before transport or repeated compilation.

Public data-class constructors remain available for AST representation, but they can bypass factory
validation. Propagate `Failed`/`Fatal` unchanged and let backend adapters translate native failures
at the solver boundary.

## Source Map

| File | Contents |
| --- | --- |
| [`IntegerDomain.kt`](IntegerDomain.kt) | Exact domains and bounded enumeration |
| [`ConstraintProgrammingExpression.kt`](ConstraintProgrammingExpression.kt) | Integer expression AST and checked arithmetic |
| [`BooleanLiteral.kt`](BooleanLiteral.kt) | Binary literals and Boolean evaluation |
| [`ConstraintProgrammingConstraint.kt`](ConstraintProgrammingConstraint.kt) | Logic, global, graph, sequence, and reservoir constraints |
| [`IntervalVariable.kt`](IntervalVariable.kt) | Interval, no-overlap, and cumulative semantics |
| [`ConstraintProgrammingModel.kt`](ConstraintProgrammingModel.kt) | Registry, validation, objectives, groups, and lifecycle |
| [`ConstraintProgrammingModelSnapshot.kt`](ConstraintProgrammingModelSnapshot.kt) | Frozen model and diagnostic activations |
| [`ConstraintProgrammingSnapshotCodec.kt`](ConstraintProgrammingSnapshotCodec.kt) | Versioned JSON encoding and decoding |
| [`ConstraintGroupRegistry.kt`](ConstraintGroupRegistry.kt) | Framework constraint-group integration |

Solver-facing features and SPI live in
[`core.solver.constraint_programming`](../../solver/constraint_programming/ConstraintProgrammingSolver.kt).
