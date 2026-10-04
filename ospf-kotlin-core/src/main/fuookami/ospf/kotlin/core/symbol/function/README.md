# function — Function Symbol Library

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `function` sub-package provides **function symbols** for optimization expressions and constraints. The catalog includes exact MILP formulations, LP relaxations, target-direction-dependent epigraphs, and piecewise-linear approximations; their model guarantees differ and are documented per function. Function symbols support evaluation and dependency/bound management through the intermediate-symbol APIs.

## Function Symbol Catalog

### Functions with quadratic inputs

`FunctionSymbolLifecycle<V>` shares auxiliary-token registration.
`MathFunctionSymbolBase<V>` retains linear constraint registration; the public
`QuadraticMathFunctionSymbolBase<V>` registers quadratic constraints.
Existing callers do not need migration.

The new counterparts are `QuadraticMaxFunction`, `QuadraticAbsFunction`,
`QuadraticMinMaxFunction`, `QuadraticMaxMinFunction`, `QuadraticMaskingFunction`,
`QuadraticIfFunction`, `QuadraticIfInFunction`, `QuadraticIfThenFunction`, and
`QuadraticInequalityFunction`. They implement `QuadraticIntermediateSymbol`:
add them to `QuadraticMetaModel`, use their `polynomial` in objectives or
constraints, and evaluate them from original inputs. Existing quadratic Min,
PositivePart, Slack, SlackRange, and InStepRange implementations remain unchanged.

`QuadraticFunctionSymbol` explicitly composes each selected linear counterpart.
Each genuinely quadratic input adds one signed real bridge variable and one
exact quadratic equality. Affine inputs need no binding variables. Functions
whose linear formulation uses Big-M additionally need finite source bounds. This avoids
cubic or quartic expansion in Masking and IfThen. The resulting formulation may
be nonconvex MIQCP and requires a solver supporting nonconvex quadratic constraints;
convexity and performance are not guaranteed.

Inputs require inferable finite bounds; arbitrary default Big-M values do not
substitute for unknown ranges. Widening bounds after their first use is rejected
during registration; tightening remains safe. Masking requires a `BinVar`.
If/IfIn/IfThen preserve the linear counterparts' boundary gaps and delta;
Inequality preserves its tolerance and strict boundary. Evaluation inside a gap
may return null. IfThen has a zero false branch. MinMax/MaxMin are exact extrema
independent of objective direction.

These compositions expand at MechanismModel and do not forward structures to
linear native adapters. No quadratic approximation policy is added for PWL/SOS
or Sin/Cos/Sigmoid. Logic over binary variables already works in quadratic models
through linear constraints and needs no duplicate implementation.

Linear-input functions can also be registered in a quadratic model when their
generated rows are linear. `ProductFunction` expands two linear expressions into
a quadratic expression and creates no product-result variable.
`QuadraticLinearFunction` conditionally binds a quadratic expression to a signed
helper variable with an equality so it can be reused as a linear value; it is
not a MILP linearization. Both are quadratic-model operations and may require a
solver supporting quadratic expressions or MIQCP constraints.

```kotlin
val absolute = QuadraticAbsFunction(
    polynomial = quadraticInput,
    converter = converter,
    name = "absolute"
)
model.add(absolute)
model.minimize(absolute.polynomial)
```

### Products, selection, lookup, and order statistics

| File | Symbol | Description |
|------|--------|-------------|
| `IntegerProduct.kt` | `IntegerProductFunction` | Exact product of a bounded integer variable and a bounded linear expression, using binary expansion and masking |
| `Select.kt` | `SelectFunction` / `IfThenElseFunction` | Exact two-branch value selection by a binary variable |
| `McCormickEnvelope.kt` | `McCormickEnvelopeFunction` | Four-plane LP relaxation for the product of two bounded linear expressions |
| `Element.kt` | `ElementFunction` / `LookupFunction` | Select a linear expression from a table using a bounded integer index |
| `OrderStatistic.kt` | `ArgMinFunction`, `ArgMaxFunction` | Return an extremum's index; solver ties may select any optimum, semantic evaluation uses the smallest index |
| `OrderStatistic.kt` | `KthLargestFunction` | Exact zero-based k-th largest value |
| `OrderStatistic.kt` | `TopKSumFunction` | Exact sum of the k largest values for `0 <= k <= n` |

### Modeling compositions and risk

| File | Symbol | Description |
|------|--------|-------------|
| `CompositeFunctions.kt` | `PositivePartFunction`, `ClampFunction`, `DeadZoneFunction` | Exact positive-part, closed-interval clipping, and `max(|x| - delta, 0)` |
| `Complementarity.kt` | `ComplementarityFunction` / `MutuallyExclusivePositiveFunction` | Exact at-most-one-positive constraint for two bounded non-negative expressions |
| `CompositeFunctions.kt` | `L1DistanceFunction`, `LInfinityDistanceFunction`, `RangeFunction` | Exact vector distances and maximum-minus-minimum range |
| `Cardinality.kt` | `AtMostFunction`, `ExactlyFunction`, `atMostConstraints`, `exactlyConstraints` | Count binary indicators with an upper bound or exact total, with indicator and pure-row forms |
| `Cvar.kt` | `CvarFunction`, `CvarEpigraphFunction` | Exact discrete CVaR result and a separate LP epigraph for minimization/upper-bound use |

### Piecewise-linear nonlinear approximations

| File | Symbol | Description |
|------|--------|-------------|
| `NonlinearPiecewise.kt` | `ExpFunction`, `LogFunction`, `ReciprocalFunction`, `PowerFunction`, `LogisticApproximationFunction` | Piecewise-linear approximations of the exponential, logarithm, reciprocal, power, and smooth logistic functions |

These symbols interpolate the configured breakpoints exactly and approximate
the original smooth function between them. They do not provide an automatic
error bound. `SigmoidFunction` remains a discrete relation indicator, not the
smooth logistic approximation.

### Piecewise tariffs and fixed charges

| File | Composition | Description |
|------|-------------|-------------|
| `Tariff.kt` | Incremental tariff | Charge each quantity tranche at that tranche's rate |
| `Tariff.kt` | All-units discount | Apply the selected tier's rate to the full quantity |
| `Tariff.kt` | Fixed charge | Add a setup charge when the activation binary is one |

These compositions use the existing univariate piecewise-linear and masking
primitives; their pages give Kotlin and Rust construction examples.

### Constraint-programming globals

| Factory | Symbol | Description |
|---------|--------|-------------|
| `GlobalConstraintFunctions.allDifferent(...)` | `AllDifferent` | Pairwise distinct integer expressions |
| `GlobalConstraintFunctions.noOverlap(...)` | `NoOverlap` | Non-overlapping half-open intervals |
| `GlobalConstraintFunctions.cumulative(...)` | `Cumulative` | Time-dependent demand bounded by a shared capacity |

These factories create the existing CP constraint AST. CP solvers can consume
supported globals natively; MIP lowering has finite-domain/horizon requirements,
and `Cumulative` requires its lowering policy to be enabled.

### Slack and Range

| File | Symbol | Description |
|------|--------|-------------|
| `Slack.kt` | `Slack` | Slack variable, converts inequalities to equalities |
| `SlackRange.kt` | `SlackRange` | Slack variable range constraints |
| `InStepRange.kt` | `InStepRange` | Step range constraints |
| `QuadraticInStepRange.kt` | `QuadraticInStepRange` | Quadratic step range constraints |
| `Masking.kt` | `Masking` | Masking range, selectively activate/deactivate variables |
| `QuadraticMaskingRange.kt` | `QuadraticMaskingRange` | Quadratic masking range |

### Rounding and Math

| File | Symbol | Description |
|------|--------|-------------|
| `Ceiling.kt` | `Ceiling` | Ceiling (integer division) |
| `Floor.kt` | `Floor` | Floor |
| `Rounding.kt` | `Rounding` | Rounding (nearest integer) |
| `Abs.kt` | `Abs` | Absolute value |
| `Mod.kt` | `Mod` | Modulo operation |
| `Product.kt` | `ProductFunction` | Quadratic expression expanding two linear expressions; no MILP linearization or standalone result variable |
| `Sigmoid.kt` | `SigmoidFunction` | Relation indicator returning 0/1 outside its undefined boundary gap; not a smooth logistic curve |
| `Sin.kt` | `Sin` | Sine function |
| `Cos.kt` | `Cos` | Cosine function |

### Min/Max

| File | Symbol | Description |
|------|--------|-------------|
| `Max.kt` | `Max` | Maximum value |
| `MinMax.kt` | `MinMax` | Minimum / maximum combination |
| `QuadraticMin.kt` | `QuadraticMin` | Quadratic minimum |
| `First.kt` | `First` | First value satisfying a condition |

### Conditional and Logic

| File | Symbol | Description |
|------|--------|-------------|
| `If.kt` | `IfFunction` | Relation indicator: 1 on the true branch and 0 on the false branch |
| `IfIn.kt` | `IfIn` | Interval condition: y = 1 iff a <= x <= b, otherwise y = 0 |
| `IfThen.kt` | `IfThen` | Conditional value: result = thenPoly when the condition holds, otherwise 0 |
| `And.kt` | `And` | Logical AND |
| `Imply.kt` | `Imply` | Logical implication |
| `OneOf.kt` | `OneOf` | Exactly one true (generalized XOR) |
| `SameAs.kt` | `SameAs` | Two variables share the same truth value |
| `SatisfiedAmount.kt` | `SatisfiedAmount` | Count of satisfied conditions |
| `SatisfiedAmountInequality.kt` | `SatisfiedAmountInequality` | Count of satisfied inequality conditions |

### Variable Conversion

| File | Symbol | Description |
|------|--------|-------------|
| `Binaryzation.kt` | `Binaryzation` | Binary representation of continuous/integer variables |
| `BalanceTernaryzation.kt` | `BalanceTernaryzation` | Balanced ternary representation |
| `Semi.kt` | `Semi` | Semi-continuous / semi-integer variables |
| `QuadraticLinear.kt` | `QuadraticLinearFunction` | Conditional bridge `y = p(x)` for reusing a quadratic expression; the quadratic equality makes the model MIQCP |

### Piecewise Linear

| File | Symbol | Description |
|------|--------|-------------|
| `UnivariateLinearPiecewise.kt` | `UnivariateLinearPiecewise` | Univariate piecewise linear function |
| `BivariateLinearPiecewise.kt` | `BivariateLinearPiecewise` | Bivariate piecewise linear function |

### Others

| File | Symbol | Description |
|------|--------|-------------|
| `BigM.kt` | `BigM` | Big-M method constraints |
| `Inequality.kt` | `Inequality` | Generic inequality constraints |
| `FunctionSymbol.kt` | `FunctionSymbol` | Function symbol base interface |

## Usage Examples

Function symbols are typically used through intermediate symbol expressions in model definitions:

```kotlin
// Create slack variable
val slack = Slack("my_slack", lowerBound = Flt64.zero, upperBound = Flt64(100.0))

// Binary-gated value selection
val selected = SelectFunction(
    mask = active,
    then = thenPolynomial,
    otherwise = otherwisePolynomial,
    converter = converter,
    name = "selected"
)
```

## Design Principles

- All function symbols implement the `IntermediateSymbol<V>` interface
- Evaluation via `prepare()` / `evaluate()` methods
- Bound information provided through `range`
- Evaluation results cached for performance
- Dependency tracking via `dependencies`
