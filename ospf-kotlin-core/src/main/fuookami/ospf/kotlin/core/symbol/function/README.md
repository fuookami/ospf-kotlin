# function — Function Symbol Library

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `function` sub-package provides a collection of **function symbols** for building optimization constraints in the OSPF framework. Each function symbol encapsulates a common mathematical/logical operation and can be directly used in model constraint expressions. All function symbols implement the `IntermediateSymbol` interface, supporting evaluation, caching, and bound management.

## Function Symbol Catalog

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
| `IfIn.kt` | `IfIn` | Value-in-set condition |
| `IfThen.kt` | `IfThen` | Implication `if A then B` |
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