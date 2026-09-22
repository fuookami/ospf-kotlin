# function — Function Symbol Library

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `function` sub-package provides a collection of **function symbols** for building optimization constraints in the OSPF framework. Each function symbol encapsulates a common mathematical/logical operation and can be directly used in model constraint expressions. All function symbols implement the `IntermediateSymbol` interface, supporting evaluation, caching, and bound management.

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
Each input containing quadratic terms adds one bounded real variable and one
exact quadratic equality. Affine inputs need no binding variables. This avoids
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

```kotlin
val absolute = QuadraticAbsFunction(
    polynomial = quadraticInput,
    converter = converter,
    name = "absolute"
)
model.add(absolute)
model.minimize(absolute.polynomial)
```

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
| `Product.kt` | `Product` | Product operation |
| `Sigmoid.kt` | `Sigmoid` | Sigmoid function |
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
| `If.kt` | `If` | Conditional expression `if (cond) then a else b` |
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
| `QuadraticLinear.kt` | `QuadraticLinear` | Quadratic linearization |

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

// Conditional constraint
val condition = If("is_active", conditionExpr, thenExpr, elseExpr)

// Maximum value
val maxVal = Max("max_x_y", listOf(x, y))
```

## Design Principles

- All function symbols implement the `IntermediateSymbol<V>` interface
- Evaluation via `prepare()` / `evaluate()` methods
- Bound information provided through `range`
- Evaluation results cached for performance
- Dependency tracking via `dependencies`
